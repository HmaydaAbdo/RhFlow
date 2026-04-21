package com.hrflow.fichedeposte.service;

import com.hrflow.direction.entities.Direction;
import com.hrflow.direction.repositories.DirectionRepository;
import com.hrflow.direction.exception.DirectionNotFoundException;
import com.hrflow.fichedeposte.specs.FicheDePosteSpecification;
import com.hrflow.fichedeposte.dto.FicheDePosteRequest;
import com.hrflow.fichedeposte.dto.FicheDePosteResponse;
import com.hrflow.fichedeposte.dto.FicheDePosteSummaryResponse;
import com.hrflow.fichedeposte.dto.FicheDePosteSearchDto;
import com.hrflow.fichedeposte.exception.FicheDePosteAccessDeniedException;
import com.hrflow.fichedeposte.exception.FicheDePosteNotFoundException;
import com.hrflow.fichedeposte.mapper.FicheDePosteMapper;
import com.hrflow.fichedeposte.model.FicheDePoste;
import com.hrflow.fichedeposte.repositories.FicheDePosteRepository;
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

import java.util.List;

@Service
public class FicheDePosteService {

    private static final Logger log = LoggerFactory.getLogger(FicheDePosteService.class);

    private final FicheDePosteRepository ficheDePosteRepository;
    private final FicheDePosteMapper     ficheDePosteMapper;
    private final DirectionRepository    directionRepository;
    private final UserRepository         userRepository;

    public FicheDePosteService(FicheDePosteRepository ficheDePosteRepository,
                               FicheDePosteMapper ficheDePosteMapper,
                               DirectionRepository directionRepository,
                               UserRepository userRepository) {
        this.ficheDePosteRepository = ficheDePosteRepository;
        this.ficheDePosteMapper     = ficheDePosteMapper;
        this.directionRepository    = directionRepository;
        this.userRepository         = userRepository;
    }

    // =====================================================================
    // READ — liste paginée avec filtres + scoping par rôle
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<FicheDePosteSummaryResponse> search(FicheDePosteSearchDto search, Pageable pageable) {
        User currentUser = getAuthenticatedUser();
        Specification<FicheDePoste> spec = buildSpecForCurrentUser(currentUser, search);
        return ficheDePosteRepository.findAll(spec, pageable)
                .map(ficheDePosteMapper::toSummary);
    }

    // =====================================================================
    // READ — détail avec contrôle d'accès pour le DIRECTEUR
    // =====================================================================

    @Transactional(readOnly = true)
    public FicheDePosteResponse findById(Long id) {
        FicheDePoste fiche = ficheDePosteRepository.findWithDirectionById(id)
                .orElseThrow(() -> new FicheDePosteNotFoundException(id));

        User currentUser = getAuthenticatedUser();

        if (isDirecteurOnly(currentUser)) {
            assertFicheBelongsToDirecteur(fiche, currentUser);
        }

        return ficheDePosteMapper.toResponse(fiche);
    }

    @Transactional(readOnly = true)
    public Page<FicheDePosteSummaryResponse> findByDirection(Long directionId, Pageable pageable) {
        if (!directionRepository.existsById(directionId)) {
            throw new DirectionNotFoundException(directionId);
        }
        return ficheDePosteRepository
                .findByDirectionId(directionId, pageable)
                .map(ficheDePosteMapper::toSummary);
    }

    // =====================================================================
    // CREATE / UPDATE / DELETE — réservés DRH / ADMIN (garanti par @PreAuthorize)
    // =====================================================================

    @Transactional
    public FicheDePosteResponse create(FicheDePosteRequest request) {
        Direction direction = resolveDirection(request.directionId());
        FicheDePoste fiche = ficheDePosteMapper.toEntity(request);
        fiche.setDirection(direction);
        FicheDePoste saved = ficheDePosteRepository.save(fiche);
        log.info("Fiche de poste créée : id={}, intitule={}", saved.getId(), saved.getIntitulePoste());
        return ficheDePosteMapper.toResponse(saved);
    }

    @Transactional
    public FicheDePosteResponse update(Long id, FicheDePosteRequest request) {
        FicheDePoste fiche = ficheDePosteRepository.findWithDirectionById(id)
                .orElseThrow(() -> new FicheDePosteNotFoundException(id));
        Direction direction = resolveDirection(request.directionId());
        ficheDePosteMapper.updateEntity(request, fiche);
        fiche.setDirection(direction);
        FicheDePoste saved = ficheDePosteRepository.save(fiche);
        log.info("Fiche de poste mise à jour : id={}", saved.getId());
        return ficheDePosteMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!ficheDePosteRepository.existsById(id)) {
            throw new FicheDePosteNotFoundException(id);
        }
        ficheDePosteRepository.deleteById(id);
        log.info("Fiche de poste supprimée : id={}", id);
    }

    // =====================================================================
    // HELPERS PRIVÉS
    // =====================================================================

    /**
     * Construit la Specification en fonction du rôle de l'utilisateur connecté.
     * - DRH / ADMIN (même si aussi DIRECTEUR) → accès global, priorité absolue
     * - DIRECTEUR seul → restreint aux directions qu'il gère
     */
    private Specification<FicheDePoste> buildSpecForCurrentUser(User user, FicheDePosteSearchDto search) {
        if (hasRole(user, "DRH") || hasRole(user, "ADMIN")) {
            return FicheDePosteSpecification.fromSearch(search);
        }
        if (isDirecteur(user)) {
            List<Long> directionIds = directionRepository.findIdsByDirecteurId(user.getId());
            Specification<FicheDePoste> perimetre = FicheDePosteSpecification.inDirections(directionIds);
            return perimetre.and(FicheDePosteSpecification.fromSearch(search));
        }
        return FicheDePosteSpecification.fromSearch(search);
    }

    /**
     * Vérifie que la fiche appartient à une direction gérée par le directeur.
     * Lève une FicheDePosteAccessDeniedException (403) si ce n'est pas le cas.
     */
    private void assertFicheBelongsToDirecteur(FicheDePoste fiche, User directeur) {
        List<Long> directionIds = directionRepository.findIdsByDirecteurId(directeur.getId());
        if (!directionIds.contains(fiche.getDirection().getId())) {
            throw new FicheDePosteAccessDeniedException(
                    "Accès refusé : cette fiche de poste n'appartient pas à votre direction"
            );
        }
    }

    /** Vrai uniquement si DIRECTEUR sans DRH ni ADMIN. */
    private boolean isDirecteurOnly(User user) {
        return hasRole(user, "DIRECTEUR") && !hasRole(user, "DRH") && !hasRole(user, "ADMIN");
    }

    private boolean isDirecteur(User user) {
        return hasRole(user, "DIRECTEUR");
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
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UserNotFoundException(auth.getName()));
    }

    private Direction resolveDirection(Long directionId) {
        return directionRepository.findById(directionId)
                .orElseThrow(() -> new DirectionNotFoundException(directionId));
    }
}
