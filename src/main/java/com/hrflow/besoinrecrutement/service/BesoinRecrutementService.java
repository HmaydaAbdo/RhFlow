package com.hrflow.besoinrecrutement.service;

import com.hrflow.besoinrecrutement.dto.*;
import com.hrflow.besoinrecrutement.exception.BesoinRecrutementAccessDeniedException;
import com.hrflow.besoinrecrutement.exception.BesoinRecrutementConflictException;
import com.hrflow.besoinrecrutement.exception.BesoinRecrutementNotFoundException;
import com.hrflow.besoinrecrutement.mapper.BesoinRecrutementMapper;
import com.hrflow.besoinrecrutement.model.BesoinRecrutement;
import com.hrflow.besoinrecrutement.model.StatutBesoin;
import com.hrflow.besoinrecrutement.repositories.BesoinRecrutementRepository;
import com.hrflow.besoinrecrutement.specifications.BesoinRecrutementSpecification;
import com.hrflow.direction.repositories.DirectionRepository;
import com.hrflow.fichedeposte.exception.FicheDePosteNotFoundException;
import com.hrflow.fichedeposte.model.FicheDePoste;
import com.hrflow.fichedeposte.repositories.FicheDePosteRepository;
import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import com.hrflow.projetrecrutement.repositories.ProjetRecrutementRepository;
import com.hrflow.users.entities.User;
import com.hrflow.users.exception.UserNotFoundException;
import com.hrflow.users.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BesoinRecrutementService {

    private static final Logger log = LoggerFactory.getLogger(BesoinRecrutementService.class);

    private final BesoinRecrutementRepository besoinRepository;
    private final BesoinRecrutementMapper     besoinMapper;
    private final FicheDePosteRepository      ficheDePosteRepository;
    private final DirectionRepository         directionRepository;
    private final UserRepository              userRepository;
    private final ProjetRecrutementRepository projetRepository;

    public BesoinRecrutementService(
            BesoinRecrutementRepository besoinRepository,
            BesoinRecrutementMapper besoinMapper,
            FicheDePosteRepository ficheDePosteRepository,
            DirectionRepository directionRepository,
            UserRepository userRepository,
            ProjetRecrutementRepository projetRepository) {
        this.besoinRepository       = besoinRepository;
        this.besoinMapper           = besoinMapper;
        this.ficheDePosteRepository = ficheDePosteRepository;
        this.directionRepository    = directionRepository;
        this.userRepository         = userRepository;
        this.projetRepository       = projetRepository;
    }

    // =====================================================================
    // CREATE — réservé au DIRECTEUR
    // =====================================================================

    @Transactional
    public BesoinRecrutementResponse create(BesoinRecrutementRequest request) {
        User directeur = getAuthenticatedUser();

        FicheDePoste fiche = resolveFicheDePoste(request.ficheDePosteId());

        // Le directeur ne peut exprimer un besoin que pour ses propres directions
        assertFicheBelongsToDirecteur(fiche, directeur);

        BesoinRecrutement besoin = besoinMapper.toEntity(request);
        besoin.setFicheDePoste(fiche);
        besoin.setDirecteur(directeur);
        besoin.setStatut(StatutBesoin.EN_COURS);

        BesoinRecrutement saved = besoinRepository.save(besoin);
        log.info("Besoin en recrutement créé : id={}, ficheId={}, directeurId={}",
                saved.getId(), fiche.getId(), directeur.getId());

        return toFullResponse(saved);
    }

    // =====================================================================
    // UPDATE — le Directeur peut modifier à tout moment son besoin
    // =====================================================================

    @Transactional
    public BesoinRecrutementResponse update(Long id, BesoinRecrutementRequest request) {
        User directeur = getAuthenticatedUser();
        BesoinRecrutement besoin = loadWithDetails(id);

        // Si la fiche change, re-valider l'appartenance et l'unicité
        if (!besoin.getFicheDePoste().getId().equals(request.ficheDePosteId())) {
            FicheDePoste nouvelleFiche = resolveFicheDePoste(request.ficheDePosteId());
            assertFicheBelongsToDirecteur(nouvelleFiche, directeur);

            boolean conflitExiste = besoinRepository.existsByFicheDePosteIdAndStatutAndIdNot(
                    nouvelleFiche.getId(), StatutBesoin.EN_COURS, id);
            if (conflitExiste) {
                throw new BesoinRecrutementConflictException(nouvelleFiche.getId());
            }
            besoin.setFicheDePoste(nouvelleFiche);
        }

        besoinMapper.updateEntity(request, besoin);
        BesoinRecrutement saved = besoinRepository.save(besoin);
        log.info("Besoin en recrutement mis à jour : id={}", saved.getId());

        return toFullResponse(saved);
    }

    // =====================================================================
    // DECISION — réservé au DRH
    // =====================================================================

    @Transactional
    public BesoinRecrutementResponse decider(Long id, DecisionBesoinRequest request) {
        BesoinRecrutement besoin = loadWithDetails(id);

        if (request.statut() == StatutBesoin.ACCEPTE) {
            // Créer le projet de recrutement associé
            ProjetRecrutement projet = new ProjetRecrutement();
            projet.setBesoinRecrutement(besoin);
            projet.setFicheDePoste(besoin.getFicheDePoste());
            projet.setNombrePostes(besoin.getNombrePostes());
            projetRepository.save(projet);

            // Archiver le besoin (il ne doit plus apparaître dans la liste active)
            besoin.setStatut(StatutBesoin.ARCHIVE);
            besoin.setMotifRefus(null);

            log.info("Besoin id={} accepté → ProjetRecrutement créé, besoin archivé", id);
        } else {
            besoin.setStatut(StatutBesoin.REFUSE);
            besoin.setMotifRefus(request.motifRefus());
            log.info("Décision DRH sur besoin id={} : REFUSE", id);
        }

        BesoinRecrutement saved = besoinRepository.save(besoin);
        return toFullResponse(saved);
    }

    // =====================================================================
    // READ — liste paginée avec filtres
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<BesoinRecrutementSummaryResponse> search(
            BesoinRecrutementSearchDto search,
            Pageable pageable) {

        User currentUser = getAuthenticatedUser();
        Specification<BesoinRecrutement> spec = buildSpecForCurrentUser(currentUser, search);

        return besoinRepository.findAll(spec, pageable)
                .map(besoinMapper::toSummary);
    }

    // =====================================================================
    // READ — détail
    // =====================================================================

    @Transactional(readOnly = true)
    public BesoinRecrutementResponse findById(Long id) {
        BesoinRecrutement besoin = loadWithDetails(id);
        return toFullResponse(besoin);
    }

    // =====================================================================
    // DELETE — le Directeur supprime son besoin
    // =====================================================================

    @Transactional
    public void delete(Long id) {
        User directeur = getAuthenticatedUser();
        BesoinRecrutement besoin = besoinRepository.findById(id)
                .orElseThrow(() -> new BesoinRecrutementNotFoundException(id));

        besoinRepository.delete(besoin);
        log.info("Besoin en recrutement supprimé : id={}", id);
    }

    // =====================================================================
    // STATS DASHBOARD — réservé au DRH / ADMIN
    // =====================================================================

    @Transactional(readOnly = true)
    public BesoinStatsResponse getStats() {
        long total    = besoinRepository.count();
        long enCours  = besoinRepository.countByStatut(StatutBesoin.EN_COURS);
        long acceptes = besoinRepository.countByStatut(StatutBesoin.ACCEPTE);
        long refuses  = besoinRepository.countByStatut(StatutBesoin.REFUSE);

        // Agrège les lignes (directionId, directionNom, statut, count) par direction
        Map<Long, long[]> dirMap = new LinkedHashMap<>();
        Map<Long, String> dirNoms = new LinkedHashMap<>();
        for (Object[] row : besoinRepository.countGroupByDirectionAndStatut()) {
            Long dirId       = ((Number) row[0]).longValue();
            String dirNom    = (String) row[1];
            StatutBesoin st  = (StatutBesoin) row[2];
            long count       = ((Number) row[3]).longValue();
            dirNoms.put(dirId, dirNom);
            long[] counts = dirMap.computeIfAbsent(dirId, k -> new long[3]);
            if (st == StatutBesoin.EN_COURS) counts[0] += count;
            else if (st == StatutBesoin.ACCEPTE) counts[1] += count;
            else if (st == StatutBesoin.REFUSE)  counts[2] += count;
        }
        List<BesoinStatsResponse.StatParDirection> parDirection = new ArrayList<>();
        dirMap.forEach((dirId, counts) -> parDirection.add(
                new BesoinStatsResponse.StatParDirection(
                        dirId, dirNoms.get(dirId),
                        counts[0] + counts[1] + counts[2],
                        counts[0], counts[1], counts[2]
                )
        ));

        List<BesoinStatsResponse.StatParPriorite> parPriorite = besoinRepository
                .countGroupByPriorite()
                .stream()
                .map(row -> new BesoinStatsResponse.StatParPriorite(
                        row[0].toString(),
                        ((Number) row[1]).longValue()
                ))
                .toList();

        return new BesoinStatsResponse(total, enCours, acceptes, refuses, parDirection, parPriorite);
    }



    // =====================================================================
    // HELPERS PRIVÉS
    // =====================================================================

    private BesoinRecrutement loadWithDetails(Long id) {
        return besoinRepository.findWithDetailsById(id)
                .orElseThrow(() -> new BesoinRecrutementNotFoundException(id));
    }

    private FicheDePoste resolveFicheDePoste(Long ficheDePosteId) {
        return ficheDePosteRepository.findWithDirectionById(ficheDePosteId)
                .orElseThrow(() -> new FicheDePosteNotFoundException(ficheDePosteId));
    }

    /**
     * Construit la Specification en fonction du rôle et du flag mineOnly.
     *
     * - mineOnly=true  → filtre sur directeur_id = user courant, quel que soit le rôle
     *                    (utilisé par "Mes recrutements")
     * - mineOnly=false + DRH/ADMIN → accès global (utilisé par "Toutes les demandes")
     * - mineOnly=false + DIRECTEUR → restreint à ses directions
     */
    private Specification<BesoinRecrutement> buildSpecForCurrentUser(
            User user,
            BesoinRecrutementSearchDto search) {

        if (search.mineOnly()) {
            // "Mes recrutements" : uniquement les besoins créés par l'utilisateur connecté
            return BesoinRecrutementSpecification.belongsToDirecteur(user.getId())
                    .and(BesoinRecrutementSpecification.fromSearch(search));
        }

        boolean isDrhOrAdmin = hasRole(user, "DRH") || hasRole(user, "ADMIN");

        if (isDrhOrAdmin) {
            // "Toutes les demandes" : accès global pour DRH / ADMIN
            return BesoinRecrutementSpecification.fromSearch(search);
        }

        // DIRECTEUR sans mineOnly : restreindre à ses directions
        List<Long> directionIds = directionRepository.findIdsByDirecteurId(user.getId());
        Specification<BesoinRecrutement> perimetre =
                BesoinRecrutementSpecification.inDirections(directionIds);

        return perimetre.and(BesoinRecrutementSpecification.fromSearch(search));
    }

    /**
     * Vérifie que la fiche de poste appartient à une direction gérée par le directeur.
     */
    private void assertFicheBelongsToDirecteur(FicheDePoste fiche, User directeur) {
        List<Long> directionIds = directionRepository.findIdsByDirecteurId(directeur.getId());
        if (!directionIds.contains(fiche.getDirection().getId())) {
            throw new BesoinRecrutementAccessDeniedException(
                    "Vous ne pouvez exprimer un besoin que pour les fiches de poste de vos directions"
            );
        }
    }


    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(r -> roleName.equalsIgnoreCase(r.getRoleName()));
    }

    private BesoinRecrutementResponse toFullResponse(BesoinRecrutement besoin) {
        // S'assurer que les associations lazy sont bien chargées (via findWithDetailsById)
        return besoinMapper.toResponse(besoin);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UserNotFoundException(auth.getName()));
    }
}
