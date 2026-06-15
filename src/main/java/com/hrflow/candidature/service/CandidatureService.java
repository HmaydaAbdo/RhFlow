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
import com.hrflow.ingestion.config.IngestProperties;
import com.hrflow.ingestion.logging.IngestMdc;
import com.hrflow.ingestion.model.IngestionRecord;
import com.hrflow.ingestion.model.IngestionSource;
import com.hrflow.ingestion.service.IngestionRecorder;
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
    private final IngestionRecorder           ingestionRecorder;
    private final IngestProperties            ingestProperties;
    private final TransactionTemplate         txWrite;

    public CandidatureService(
            CandidatureRepository candidatureRepo,
            ProjetRecrutementRepository projetRepo,
            MinioService minioService,
            CandidatureMapper mapper,
            CvPipelineService pipeline,
            UserRepository userRepository,
            IngestionRecorder ingestionRecorder,
            IngestProperties ingestProperties,
            PlatformTransactionManager txManager) {
        this.candidatureRepo   = candidatureRepo;
        this.projetRepo        = projetRepo;
        this.minioService      = minioService;
        this.mapper            = mapper;
        this.pipeline          = pipeline;
        this.userRepository    = userRepository;
        this.ingestionRecorder = ingestionRecorder;
        this.ingestProperties  = ingestProperties;
        this.txWrite           = new TransactionTemplate(txManager);
    }

    // ── Upload manuel (UI) ──────────────────────────────────────────────────────

    /**
     * Upload d'un CV pour un projet de recrutement depuis l'UI (DRH/ADMIN/DIRECTEUR).
     *
     * <p>Étapes :
     * <ol>
     *   <li>Validation du fichier (pages, format) — early return 400 si invalide.</li>
     *   <li>Résolution du projet + check d'ownership DIRECTEUR (TX courte).</li>
     *   <li><b>Création d'un {@code IngestionRecord} PENDING</b> avec
     *       {@code source=MANUAL_UI} — c'est la trace dans le journal d'entrée
     *       unique, partagée avec les ingestions automatiques (n8n IMAP, etc.).</li>
     *   <li>Délégation à {@link #createCandidature} (persistance + MinIO + pipeline).</li>
     *   <li>Si succès → record passe à {@code IMPORTED} avec la FK candidature.
     *       Si erreur → record passe à {@code ERROR}, l'exception remonte au client.</li>
     * </ol>
     *
     * <p>Le flux MinIO + pipeline est documenté dans {@link #createCandidature}.
     */
    public CandidatureResponse upload(Long projetId, MultipartFile file) {

        validateFileSize(file);     // soft cap configurable (app.ingest.max-file-size)
        validatePageCount(file);    // limite métier sur le nombre de pages

        // TX courte : résolution du projet + check d'ownership DIRECTEUR.
        // On charge le projet dans une TX dédiée pour libérer la connexion avant MinIO.
        ProjetRecrutement projet = txWrite.execute(status -> {
            var p = projetRepo.findWithDetailsById(projetId)
                    .orElseThrow(() -> new ProjetRecrutementNotFoundException(projetId));
            enforceDirecteurOwnership(p);
            return p;
        });

        // Création du IngestionRecord PENDING — trace dans le journal d'entrée.
        // externalId synthétique (UUID) car pas de Message-ID naturel pour l'upload manuel.
        IngestionRecord record = createPendingRecord(file);

        // MDC pour la traçabilité : à partir d'ici tous les logs du flux portent
        // source + externalId + recordId. Cleanup garanti par try-finally.
        try {
            IngestMdc.put(IngestionSource.MANUAL_UI.name(),
                          record.getExternalId(),
                          String.valueOf(record.getId()));
            try {
                Candidature saved = createCandidature(projet, file);
                markRecordImported(record, saved);
                return mapper.toResponse(saved);
            } catch (RuntimeException e) {
                markRecordError(record, e.getMessage());
                throw e;   // l'utilisateur DOIT voir l'erreur côté UI
            }
        } finally {
            IngestMdc.clear();
        }
    }

    // ── Helpers IngestionRecord pour upload manuel ──────────────────────────────

    /**
     * Crée un {@link IngestionRecord} PENDING pour un upload manuel.
     * {@code externalId} = "manual-{UUID}" — pas de risque de collision avec les
     * Message-IDs des emails (qui contiennent un {@code @}).
     * {@code rawMetadata} = JSON avec l'email de l'utilisateur connecté, pour audit.
     */
    private IngestionRecord createPendingRecord(MultipartFile file) {
        String externalId = "manual-" + UUID.randomUUID();
        String userEmail  = currentUserEmail();
        String rawMeta    = "{\"uploadedBy\":\"%s\"}".formatted(userEmail != null ? userEmail : "");

        return ingestionRecorder.createPending(
                IngestionSource.MANUAL_UI,
                externalId,
                null,                                // pas de code de référence pour un upload manuel
                file.getOriginalFilename(),
                rawMeta
        );
    }

    /** Marque le record IMPORTED avec lien vers la candidature créée. */
    private void markRecordImported(IngestionRecord record, Candidature candidature) {
        ingestionRecorder.markImported(record.getId(), candidature);
    }

    /**
     * Marque le record ERROR avec le détail de l'échec.
     *
     * <p>Best-effort : si on n'arrive même plus à écrire en base, on log mais
     * on ne masque pas l'exception originale qui a déjà détruit le flow.
     */
    private void markRecordError(IngestionRecord record, String detail) {
        try {
            ingestionRecorder.markError(
                    record.getId(),
                    "Erreur upload manuel : " + (detail != null ? detail : "(inconnue)"));
        } catch (Exception ex) {
            log.error("[Candidature] impossible de marquer IngestionRecord id={} en ERROR : {}",
                    record.getId(), ex.getMessage());
        }
    }

    // ── Création de candidature (commun upload manuel + ingest n8n) ─────────────

    /**
     * Crée une candidature à partir d'un projet de recrutement déjà résolu.
     *
     * <p>Méthode <b>publique</b> car appelée cross-package par {@code IngestionService}
     * (flux n8n IMAP). Le caller a la responsabilité de :
     * <ul>
     *   <li>Avoir résolu le projet selon sa propre logique (id, objet, etc.).</li>
     *   <li>Avoir validé les autorisations (ownership pour l'UI, authority pour l'ingest).</li>
     *   <li>Avoir validé le fichier (page count, format) — typiquement via {@link #validatePageCount}.</li>
     * </ul>
     *
     * <p>Ordre d'exécution intentionnel :
     * <ol>
     *   <li>TX (courte) : créer Candidature RECU → commit → connexion libérée.</li>
     *   <li>MinIO upload (hors TX) — si échec : suppression compensatoire en base.</li>
     *   <li>{@code pipeline.traiter()} déclenché APRÈS l'upload MinIO réussi.</li>
     * </ol>
     *
     * <p>Pourquoi le pipeline est déclenché après MinIO (et non via afterCommit) :
     * afterCommit() se déclenche quand la TX commit, AVANT que minioService.upload()
     * soit appelé. Le pipeline génèrerait une presigned URL d'un objet inexistant →
     * Docling échouerait. En déclenchant pipeline.traiter() après upload, l'objet
     * est toujours présent côté MinIO.
     *
     * @param projet projet de recrutement déjà chargé (avec fiche de poste eagerly fetch)
     * @param file   fichier CV à uploader
     * @return l'entité Candidature persistée (le caller décide comment l'exposer / la lier)
     */
    public Candidature createCandidature(ProjetRecrutement projet, MultipartFile file) {

        // TX 1 : persistance candidature — connexion libérée à la sortie du lambda
        Candidature saved = txWrite.execute(status -> {
            String nomFichier  = safeFileName(file.getOriginalFilename());
            String typeFichier = file.getContentType() != null
                    ? file.getContentType() : "application/octet-stream";

            // Chemin : cvs/{slug-intitulé}/{UUID}/{nomFichierOriginal}
            // - slug   : intitulé du poste normalisé (sans accents, sans espaces)
            // - UUID   : sous-dossier unique par upload → pas d'écrasement si même nom de fichier
            // - nom    : nom original conservé tel quel pour la lisibilité dans MinIO
            String slug       = slugifyPoste(projet.getFicheDePoste().getIntitulePoste());
            String objectPath = "cvs/" + slug + "/" + UUID.randomUUID() + "/" + nomFichier;

            var candidature = new Candidature();
            candidature.setProjetRecrutement(projet);
            candidature.setNomFichier(nomFichier);
            candidature.setCheminMinio(objectPath);
            candidature.setTypeFichier(typeFichier);
            candidature.setTailleFichier(file.getSize());
            candidature.setStatut(StatutCandidature.RECU);

            Candidature c = candidatureRepo.save(candidature);
            // Flush dans la TX : détecte tout conflit de contrainte DB avant l'upload MinIO
            candidatureRepo.flush();
            return c;
        });

        // TX commitée, connexion libérée — upload MinIO
        String objectPath = saved.getCheminMinio();
        try {
            minioService.upload(objectPath, file);
            log.info("[Candidature] CV uploadé → {} (projet={})", objectPath, projet.getId());
        } catch (RuntimeException e) {
            // MinIO a échoué — supprimer la candidature orpheline en base (rollback compensatoire)
            log.error("[Candidature] upload MinIO échoué pour candidature={} — rollback DB : {}",
                    saved.getId(), e.getMessage());
            txWrite.execute(status -> { candidatureRepo.deleteById(saved.getId()); return null; });
            throw e;
        }

        // Pipeline déclenché uniquement si MinIO a réussi — @Async, retourne immédiatement
        pipeline.traiter(saved.getId());

        return saved;
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

        Sort sort = Sort.by(Sort.Order.desc("scoreMatching"))
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

        // Guard : on refuse si une évaluation est déjà en cours
        if (c.getStatut() == StatutCandidature.EN_COURS) {
            throw new IllegalArgumentException(
                    "La candidature %d est déjà en cours d'évaluation.".formatted(id));
        }

        // Reset vers RECU — obligatoire : la garde d'idempotence du pipeline
        // (CvPipelineService.traiter) n'autorise l'entrée que depuis RECU.
        // Le pipeline lui-même passera le statut EN_COURS dans sa TX1 atomique.
        c.setStatut(StatutCandidature.RECU);
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
        return minioService.presignedUrlForBrowser(c.getCheminMinio(), c.getNomFichier());
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

    // ── Validation taille fichier (soft cap métier) ─────────────────────────────

    /**
     * Vérifie que le fichier ne dépasse pas le soft cap configuré dans
     * {@code app.ingest.max-file-size} (défaut 10MB).
     *
     * <p>Au-delà : {@link IllegalArgumentException}. Le caller décide comment
     * traduire l'erreur :
     * <ul>
     *   <li>UI manuel ({@code upload()}) → laisse remonter → 400 via le handler</li>
     *   <li>Ingest n8n ({@code IngestionService.ingest()}) → catch → record en
     *       {@code REJECTED FILE_TOO_LARGE} + 200 OK</li>
     * </ul>
     *
     * <p>Pour rappel, le hard cap Spring multipart (50MB par défaut) intercepte
     * avant ce check : si le client envoie un flux > 50MB, il reçoit 413 sans
     * même arriver dans le controller.
     */
    public void validateFileSize(MultipartFile file) {
        if (file == null) return;
        long maxBytes = ingestProperties.maxFileSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "Fichier trop volumineux : %d octets reçus, maximum autorisé %d octets (%s)."
                            .formatted(file.getSize(), maxBytes, ingestProperties.maxFileSize()));
        }
    }

    // ── Validation pages ────────────────────────────────────────────────────────

    /**
     * Vérifie que le PDF/DOCX ne dépasse pas {@link #MAX_CV_PAGES} pages.
     * Lève {@link IllegalArgumentException} si dépassement — le caller décide
     * comment traduire (HTTP 400 pour l'UI, REJECTED record pour l'ingest n8n).
     */
    public void validatePageCount(MultipartFile file) {
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

    /**
     * Retourne le nom de fichier original nettoyé (sans séparateurs de chemin).
     * Repli sur "cv_<uuid>" si null ou vide.
     */
    private static String safeFileName(String original) {
        if (original == null || original.isBlank()) return "cv_" + UUID.randomUUID();
        // Retire tout chemin (ex: "../../etc/passwd") en ne gardant que le nom de base
        String name = original.replaceAll("[/\\\\]", "_").trim();
        if (name.isBlank()) return "cv_" + UUID.randomUUID();
        // Valide l'extension via la liste autorisée
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            String ext = name.substring(dot + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                name = name.substring(0, dot + 1) + "bin";
            }
        }
        return name;
    }

    /**
     * Slugifie l'intitulé du poste pour l'utiliser comme dossier MinIO.
     * Ex : "Développeur Java Sénior" → "developpeur-java-senior"
     * Les caractères hors [a-z0-9-] sont remplacés par "-", les tirets consécutifs réduits.
     */
    private static String slugifyPoste(String intitule) {
        if (intitule == null || intitule.isBlank()) return "sans-intitule";
        // 1. Normalisation NFD : décompose les caractères accentués (é → e + combining)
        String normalized = java.text.Normalizer.normalize(intitule, java.text.Normalizer.Form.NFD);
        // 2. Supprime les combinaisons diacritiques (accents)
        String ascii = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        // 3. Minuscules + remplacement des caractères non alphanumériques par "-"
        String slug = ascii.toLowerCase()
                           .replaceAll("[^a-z0-9]+", "-")
                           .replaceAll("^-+|-+$", ""); // trim tirets
        return slug.isBlank() ? "sans-intitule" : slug;
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }
}
