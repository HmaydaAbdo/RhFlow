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
import com.hrflow.projetrecrutement.service.ProjetRecrutementService;
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
    private final ProjetRecrutementService    projetService;

    public BesoinRecrutementService(
            BesoinRecrutementRepository besoinRepository,
            BesoinRecrutementMapper besoinMapper,
            FicheDePosteRepository ficheDePosteRepository,
            DirectionRepository directionRepository,
            UserRepository userRepository,
            ProjetRecrutementService projetService) {
        this.besoinRepository       = besoinRepository;
        this.besoinMapper           = besoinMapper;
        this.ficheDePosteRepository = ficheDePosteRepository;
        this.directionRepository    = directionRepository;
        this.userRepository         = userRepository;
        this.projetService          = projetService;
    }

    // =====================================================================
    // CREATE — réservé au DIRECTEUR
    // =====================================================================

    @Transactional
    public BesoinRecrutementResponse create(BesoinRecrutementRequest request) {
        User authenticatedUser = getAuthenticatedUser();

        FicheDePoste fiche = resolveFicheDePoste(request.ficheDePosteId());

        if(isDirecteurOnly(authenticatedUser)){
            // Le directeur ne peut exprimer un besoin que pour ses propres directions
            assertFicheBelongsToDirecteur(fiche, authenticatedUser);
        }


        BesoinRecrutement besoin = besoinMapper.toEntity(request);
        besoin.setFicheDePoste(fiche);
        besoin.setDirecteur(fiche.getDirection().getDirecteur());
        besoin.setCreatedBy(authenticatedUser);
        besoin.setEncours(true);
        // statut reste null jusqu'à la décision du DRH

        BesoinRecrutement saved = besoinRepository.save(besoin);
        log.info("Besoin en recrutement créé : id={}, ficheId={}, directeurId={}, createdById={}",
                saved.getId(), fiche.getId(),
                saved.getDirecteur().getId(), authenticatedUser.getId());

        return toFullResponse(saved);
    }

    // =====================================================================
    // UPDATE — modifiable uniquement si encours=true
    // =====================================================================

    @Transactional
    public BesoinRecrutementResponse update(Long id, BesoinRecrutementRequest request) {
        User directeur = getAuthenticatedUser();
        BesoinRecrutement besoin = loadWithDetails(id);

        // Un besoin décidé ne peut plus être modifié
        if (!besoin.isEncours()) {
            throw new BesoinRecrutementAccessDeniedException(
                    "Ce besoin a déjà fait l'objet d'une décision et ne peut plus être modifié");
        }

        // Si la fiche change, re-valider l'appartenance
        if (!besoin.getFicheDePoste().getId().equals(request.ficheDePosteId())) {
            FicheDePoste nouvelleFiche = resolveFicheDePoste(request.ficheDePosteId());
            if(isDirecteurOnly(directeur)){
                assertFicheBelongsToDirecteur(nouvelleFiche, directeur);
            }
            besoin.setFicheDePoste(nouvelleFiche);
        }

        besoinMapper.updateEntity(request, besoin);
        BesoinRecrutement saved = besoinRepository.save(besoin);
        log.info("Besoin en recrutement mis à jour : id={}", saved.getId());

        return toFullResponse(saved);
    }

    // =====================================================================
    // DECISION — réservé au DRH et ADMIN
    // Applicable sur encours=true (première décision) ou encours=false (changement de décision depuis l'archive).
    //
    // Règles ProjetRecrutement :
    //   → ACCEPTE (null→ACCEPTE ou REFUSE→ACCEPTE) : créer un projet si inexistant
    //   → REFUSE  (null→REFUSE  ou ACCEPTE→REFUSE) : supprimer le projet s'il existe
    // =====================================================================

    @Transactional
    public BesoinRecrutementResponse decider(Long id, DecisionBesoinRequest request) {
        BesoinRecrutement besoin = loadWithDetails(id);

        StatutBesoin ancienStatut = besoin.getStatut();

        if (request.statut() == StatutBesoin.ACCEPTE) {
            // → ACCEPTE : déléguer la création au ProjetRecrutementService
            projetService.createForBesoin(besoin);
        } else {
            // → REFUSE : déléguer la suppression au ProjetRecrutementService
            projetService.deleteByBesoinId(id);
        }

        besoin.setEncours(false);
        besoin.setStatut(request.statut());

        BesoinRecrutement saved = besoinRepository.save(besoin);
        log.info("Décision sur besoin id={} : {} (ancien statut : {})",
                id, request.statut(), ancienStatut == null ? "aucun" : ancienStatut);

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
    // DELETE
    // =====================================================================

    @Transactional
    public void delete(Long id) {
        BesoinRecrutement besoin = besoinRepository.findById(id)
                .orElseThrow(() -> new BesoinRecrutementNotFoundException(id));

        // Un besoin accepté a un projet de recrutement associé — on bloque la suppression.
        // Pour supprimer ce besoin, le DRH doit d'abord changer la décision en "Refusé"
        // (ce qui supprimera automatiquement le projet), puis supprimer le besoin.
        if (StatutBesoin.ACCEPTE.equals(besoin.getStatut())) {
            throw new BesoinRecrutementConflictException(
                    "Ce besoin est lié à un projet de recrutement actif. " );
        }

        besoinRepository.delete(besoin);
        log.info("Besoin en recrutement supprimé : id={}", id);
    }

    // =====================================================================
    // STATS DASHBOARD — réservé au DRH / ADMIN
    // =====================================================================

    @Transactional(readOnly = true)
    public BesoinStatsResponse getStats() {
        long total    = besoinRepository.count();
        long enCours  = besoinRepository.countByEncoursTrue();
        long acceptes = besoinRepository.countByStatut(StatutBesoin.ACCEPTE);
        long refuses  = besoinRepository.countByStatut(StatutBesoin.REFUSE);

        // row : [dirId, dirNom, encours(boolean), statut(StatutBesoin|null), count]
        Map<Long, long[]> dirMap  = new LinkedHashMap<>();
        Map<Long, String> dirNoms = new LinkedHashMap<>();
        for (Object[] row : besoinRepository.countGroupByDirectionAndStatut()) {
            Long   dirId       = ((Number)  row[0]).longValue();
            String dirNom      = (String)   row[1];
            boolean isEncours  = (Boolean)  row[2];
            StatutBesoin st    = row[3] != null ? (StatutBesoin) row[3] : null;
            long   count       = ((Number)  row[4]).longValue();
            dirNoms.put(dirId, dirNom);
            long[] counts = dirMap.computeIfAbsent(dirId, k -> new long[3]); // [enCours, acceptes, refuses]
            if      (isEncours)                       counts[0] += count;
            else if (st == StatutBesoin.ACCEPTE)      counts[1] += count;
            else if (st == StatutBesoin.REFUSE)       counts[2] += count;
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
        return ficheDePosteRepository.findWithDirectionAndDirecteurById(ficheDePosteId)
                .orElseThrow(() -> new FicheDePosteNotFoundException(ficheDePosteId));
    }

    /**
     * Construit la Specification en fonction du rôle et du flag mineOnly.
     *
     * - mineOnly=true  → directeur_id = user OU created_by_id = user (Mes besoins)
     * - mineOnly=false + DRH/ADMIN → accès global (Tous les besoins / Archive)
     * - mineOnly=false + DIRECTEUR → restreint à ses directions
     */
    private Specification<BesoinRecrutement> buildSpecForCurrentUser(
            User user,
            BesoinRecrutementSearchDto search) {

        if (search.mineOnly()) {
            return BesoinRecrutementSpecification.belongsToUser(user.getId())
                    .and(BesoinRecrutementSpecification.fromSearch(search));
        }

        boolean isDrhOrAdmin = hasRole(user, "DRH") || hasRole(user, "ADMIN");

        if (isDrhOrAdmin) {
            return BesoinRecrutementSpecification.fromSearch(search);
        }

        // DIRECTEUR sans mineOnly : restreindre à ses directions
        List<Long> directionIds = directionRepository.findIdsByDirecteurId(user.getId());
        return BesoinRecrutementSpecification.inDirections(directionIds)
                .and(BesoinRecrutementSpecification.fromSearch(search));
    }

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

    private boolean isDirecteurOnly(User user) {
        return hasRole(user, "DIRECTEUR") && !hasRole(user, "DRH") && !hasRole(user, "ADMIN");
    }

    private BesoinRecrutementResponse toFullResponse(BesoinRecrutement besoin) {
        return besoinMapper.toResponse(besoin);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        // findWithRolesByEmail charge les rôles en JOIN — évite le lazy load dans hasRole()
        return userRepository.findWithRolesByEmail(auth.getName())
                .orElseThrow(() -> new UserNotFoundException(auth.getName()));
    }
}
