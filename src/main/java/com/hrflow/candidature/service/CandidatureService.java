package com.hrflow.candidature.service;

import com.hrflow.candidature.dto.CandidatureResponse;
import com.hrflow.candidature.dto.CandidatureSearchDto;
import com.hrflow.candidature.dto.StatutUpdateRequest;
import com.hrflow.candidature.exception.CandidatureNotFoundException;
import com.hrflow.candidature.mapper.CandidatureMapper;
import com.hrflow.candidature.model.Candidature;
import com.hrflow.candidature.model.StatutCandidature;
import com.hrflow.candidature.repository.CandidatureRepository;
import com.hrflow.candidature.specifications.CandidatureSpecification;
import com.hrflow.projetrecrutement.exception.ProjetRecrutementNotFoundException;
import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import com.hrflow.projetrecrutement.repositories.ProjetRecrutementRepository;
import com.hrflow.storage.service.MinioService;
import com.hrflow.users.entities.User;
import com.hrflow.users.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CandidatureService {

    private static final Logger log = LoggerFactory.getLogger(CandidatureService.class);

    private static final int         MAX_CV_PAGES       = 4;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final CandidatureRepository       candidatureRepo;
    private final ProjetRecrutementRepository projetRepo;
    private final MinioService                minioService;
    private final CandidatureMapper           mapper;
    private final CvPipelineService           pipeline;
    private final UserRepository              userRepository;
    private final TransactionTemplate         txWrite;

    public CandidatureService(
            CandidatureRepository candidatureRepo,
            ProjetRecrutementRepository projetRepo,
            MinioService minioService,
            CandidatureMapper mapper,
            CvPipelineService pipeline,
            UserRepository userRepository,
            PlatformTransactionManager txManager) {
        this.candidatureRepo = candidatureRepo;
        this.projetRepo      = projetRepo;
        this.minioService    = minioService;
        this.mapper          = mapper;
        this.pipeline        = pipeline;
        this.userRepository  = userRepository;
        this.txWrite         = new TransactionTemplate(txManager);
    }

    // ── Upload ──────────────────────────────────────────────────────────────────

    /**
     * Upload d'un CV pour un projet de recrutement.
     *
     * Pattern deux transactions réel (évite le self-invocation Spring AOP) :
     *
     *   txWrite.execute() → TX ouverte, candidature persistée, TX commitée, connexion libérée
     *   [upload MinIO — aucune connexion DB tenue ici]
     *   afterCommit (enregistré dans la TX) → pipeline.traiter() lancé en async
     *
     * TransactionTemplate contourne le proxy AOP : la frontière de transaction est
     * définie programmatiquement, pas par annotation sur une méthode appelée via this.
     */
    public CandidatureResponse upload(Long projetId, MultipartFile file) {

        validatePageCount(file);

        String ext        = safeExtension(file.getOriginalFilename());
        String objectPath = "projets/" + projetId + "/cvs/" + UUID.randomUUID() + "." + ext;

        // TX 1 : périmètre explicite via TransactionTemplate.
        // La connexion DB est libérée dès la sortie du lambda (après commit).
        Candidature saved = txWrite.execute(status -> {

            var projet = projetRepo.findWithDetailsById(projetId)
                    .orElseThrow(() -> new ProjetRecrutementNotFoundException(projetId));

            enforceDirecteurOwnership(projet);

            String nomFichier  = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "cv_" + UUID.randomUUID();

            String typeFichier = file.getContentType() != null
                    ? file.getContentType() : "application/octet-stream";

            var candidature = new Candidature();
            candidature.setProjetRecrutement(projet);
            candidature.setNomFichier(nomFichier);
            candidature.setCheminMinio(objectPath);
            candidature.setTypeFichier(typeFichier);
            candidature.setTailleFichier(file.getSize());
            candidature.setStatut(StatutCandidature.RECU);

            Candidature c = candidatureRepo.save(candidature);
            // Flush dans la TX pour détecter tout conflit de contrainte DB avant l'upload MinIO
            candidatureRepo.flush();

            // afterCommit enregistré ICI (pendant la TX active) → se déclenche après commit
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pipeline.traiter(c.getId());
                }
            });

            return c;
        });

        // TX commitée, connexion libérée — upload MinIO sans connexion DB tenue
        minioService.upload(objectPath, file);
        log.info("[Candidature] CV uploadé → {} (projet={})", objectPath, projetId);

        return mapper.toResponse(saved);
    }

    // ── Liste ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CandidatureResponse> listerParProjet(Long projetId,
                                                      CandidatureSearchDto search,
                                                      int page,
                                                      int size) {

        var projet = projetRepo.findWithDetailsById(projetId)
                .orElseThrow(() -> new ProjetRecrutementNotFoundException(projetId));

        enforceDirecteurOwnership(projet);

        Sort sort = Sort.by(Sort.Order.desc("scoreMatching").nullsLast())
                        .and(Sort.by(Sort.Direction.DESC, "deposeLe"));

        int     cappedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable  = PageRequest.of(Math.max(page, 0), cappedSize, sort);

        var spec = CandidatureSpecification.fromSearch(projetId, search);
        return candidatureRepo.findAll(spec, pageable).map(mapper::toResponse);
    }

    // ── Détail ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CandidatureResponse getById(Long id) {
        var c = findWithProjet(id);
        enforceDirecteurOwnership(c.getProjetRecrutement());
        return mapper.toResponse(c);
    }

    // ── Changer statut RH ───────────────────────────────────────────────────────

    @Transactional
    public CandidatureResponse changerStatut(Long id, StatutUpdateRequest request) {
        var allowed = Set.of(StatutCandidature.RETENU, StatutCandidature.REJETE);
        if (!allowed.contains(request.statut()))
            throw new IllegalArgumentException("Seuls RETENU et REJETE peuvent être définis manuellement.");
        var c = findWithProjet(id);
        c.setStatut(request.statut());
        log.info("[Candidature] statut={} → {} par {}", id, request.statut(), currentUserEmail());
        return mapper.toResponse(candidatureRepo.save(c));
    }

    // ── Re-évaluation manuelle ───────────────────────────────────────────────────

    @Transactional
    public CandidatureResponse reevaluer(Long id) {
        var c = findWithProjet(id);
        if (c.getStatut() == StatutCandidature.EN_COURS) {
            throw new IllegalArgumentException(
                    "La candidature %d est déjà en cours d'évaluation.".formatted(id));
        }
        c.setStatut(StatutCandidature.EN_COURS);
        c.setScoreMatching(null);
        c.setPointsForts(null);
        c.setPointsManquants(null);
        c.setRecommandation(null);
        c.setJustificationIa(null);
        c.setEvalueLe(null);
        Candidature saved = candidatureRepo.save(c);

        // afterCommit : même protection race condition que dans upload()
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pipeline.traiter(id);
            }
        });

        log.info("[Candidature] re-évaluation demandée pour candidature={} par {}", id, currentUserEmail());
        return mapper.toResponse(saved);
    }

    // ── Presigned URL ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String presignedUrl(Long id) {
        var c = findWithProjet(id);
        enforceDirecteurOwnership(c.getProjetRecrutement());
        return minioService.presignedUrl(c.getCheminMinio());
    }

    // ── Supprimer ────────────────────────────────────────────────────────────────

    @Transactional
    public void supprimer(Long id) {
        var c = findWithProjet(id);
        candidatureRepo.delete(c);
        minioService.delete(c.getCheminMinio());
        log.info("[Candidature] candidature={} supprimée par {}", id, currentUserEmail());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Candidature findWithProjet(Long id) {
        return candidatureRepo.findByIdWithProjet(id)
                .orElseThrow(() -> new CandidatureNotFoundException(id));
    }

    // ── Validation pages ────────────────────────────────────────────────────────

    private void validatePageCount(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) return;
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        try {
            int pages = switch (ext) {
                case "pdf"  -> countPdfPages(file.getInputStream());
                case "docx" -> countDocxPages(file.getInputStream());
                default     -> 0;
            };
            if (pages > MAX_CV_PAGES) {
                throw new IllegalArgumentException(
                        "Le CV ne doit pas dépasser %d pages (%d pages détectées)."
                                .formatted(MAX_CV_PAGES, pages));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Candidature] Impossible de vérifier le nombre de pages de '{}' : {}",
                    filename, e.getMessage());
        }
    }

    private int countPdfPages(InputStream is) throws IOException {
        byte[] pdf = is.readAllBytes();
        int count = 0;
        for (int i = 0; i < pdf.length - 15; i++) {
            if (pdf[i] != '/' || pdf[i+1] != 'T' || pdf[i+2] != 'y'
                    || pdf[i+3] != 'p' || pdf[i+4] != 'e') continue;
            int j = i + 5;
            while (j < pdf.length && isPdfWhitespace(pdf[j])) j++;
            if (j + 5 >= pdf.length) continue;
            if (pdf[j] != '/' || pdf[j+1] != 'P' || pdf[j+2] != 'a'
                    || pdf[j+3] != 'g' || pdf[j+4] != 'e') continue;
            if (j + 5 < pdf.length && pdf[j+5] == 's') continue;
            count++;
        }
        return count;
    }

    private boolean isPdfWhitespace(byte b) {
        return b == ' ' || b == '\t' || b == '\r' || b == '\n' || b == '\f';
    }

    private int countDocxPages(InputStream inputStream) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("docProps/app.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    Matcher m = Pattern.compile("<Pages>(\\d+)</Pages>").matcher(xml);
                    if (m.find()) return Integer.parseInt(m.group(1));
                    return 0;
                }
                zip.closeEntry();
            }
        }
        return 0;
    }

    // ── Access control ──────────────────────────────────────────────────────────

    private void enforceDirecteurOwnership(ProjetRecrutement projet) {
        User currentUser = getAuthenticatedUser();
        if (isDirecteurOnly(currentUser)) {
            assertProjetBelongsToDirecteur(projet, currentUser);
        }
    }

    private void assertProjetBelongsToDirecteur(ProjetRecrutement projet, User directeur) {
        String directeurEmail = projet.getFicheDePoste().getDirection().getDirecteur().getEmail();
        if (!directeur.getEmail().equals(directeurEmail)) {
            throw new CandidatureNotFoundException(projet.getId());
        }
    }

    private boolean isDirecteurOnly(User user) {
        return hasRole(user, "DIRECTEUR") && !hasRole(user, "DRH") && !hasRole(user, "ADMIN");
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(r -> roleName.equalsIgnoreCase(r.getRoleName()));
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return userRepository.findWithRolesByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable : " + auth.getName()));
    }

    // ── File utils ──────────────────────────────────────────────────────────────

    private String safeExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : "bin";
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }
}
