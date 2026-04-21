package com.hrflow.projetrecrutement.service;

import com.hrflow.direction.repositories.DirectionRepository;
import com.hrflow.projetrecrutement.dto.ProjetRecrutementResponse;
import com.hrflow.projetrecrutement.dto.ProjetRecrutementSearchDto;
import com.hrflow.projetrecrutement.dto.ProjetRecrutementSummaryResponse;
import com.hrflow.projetrecrutement.exception.ProjetRecrutementAccessDeniedException;
import com.hrflow.projetrecrutement.exception.ProjetRecrutementAlreadyClosedException;
import com.hrflow.projetrecrutement.exception.ProjetRecrutementNotFoundException;
import com.hrflow.projetrecrutement.mapper.ProjetRecrutementMapper;
import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import com.hrflow.projetrecrutement.model.StatutProjet;
import com.hrflow.projetrecrutement.repositories.ProjetRecrutementRepository;
import com.hrflow.projetrecrutement.specifications.ProjetRecrutementSpecification;
import com.hrflow.users.entities.User;
import com.hrflow.users.exception.UserNotFoundException;
import com.hrflow.users.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjetRecrutementService {

    private static final Logger log = LoggerFactory.getLogger(ProjetRecrutementService.class);

    private final ProjetRecrutementRepository projetRepository;
    private final ProjetRecrutementMapper     projetMapper;
    private final DirectionRepository         directionRepository;
    private final UserRepository              userRepository;

    public ProjetRecrutementService(
            ProjetRecrutementRepository projetRepository,
            ProjetRecrutementMapper projetMapper,
            DirectionRepository directionRepository,
            UserRepository userRepository) {
        this.projetRepository  = projetRepository;
        this.projetMapper      = projetMapper;
        this.directionRepository = directionRepository;
        this.userRepository    = userRepository;
    }

    // =====================================================================
    // READ — liste paginée avec filtres
    // Accès : DRH/ADMIN = tout, DIRECTEUR = ses directions uniquement
    // =====================================================================

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @Transactional(readOnly = true)
    public Page<ProjetRecrutementSummaryResponse> search(
            ProjetRecrutementSearchDto search,
            Pageable pageable) {

        User currentUser = getAuthenticatedUser();
        Specification<ProjetRecrutement> spec = buildSpecForCurrentUser(currentUser, search);

        return projetRepository.findAll(spec, pageable)
                .map(projetMapper::toSummary);
    }

    // =====================================================================
    // READ — détail
    // =====================================================================

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @Transactional(readOnly = true)
    public ProjetRecrutementResponse findById(Long id) {
        User currentUser = getAuthenticatedUser();
        ProjetRecrutement projet = loadWithDetails(id);
        assertCanAccess(currentUser, projet);
        return projetMapper.toResponse(projet);
    }

    // =====================================================================
    // FERMER un projet — DRH / ADMIN uniquement
    // =====================================================================

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @Transactional
    public ProjetRecrutementResponse fermer(Long id) {
        ProjetRecrutement projet = loadWithDetails(id);

        if (projet.getStatut() == StatutProjet.FERME) {
            throw new ProjetRecrutementAlreadyClosedException(id);
        }

        projet.setStatut(StatutProjet.FERME);
        projet.setClosedAt(LocalDateTime.now());

        ProjetRecrutement saved = projetRepository.save(projet);
        log.info("Projet de recrutement fermé : id={}", saved.getId());

        return projetMapper.toResponse(saved);
    }

    // =====================================================================
    // HELPERS PRIVÉS
    // =====================================================================

    private ProjetRecrutement loadWithDetails(Long id) {
        return projetRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ProjetRecrutementNotFoundException(id));
    }

    /**
     * DRH/ADMIN : accès global.
     * DIRECTEUR : restreint à ses directions.
     */
    private Specification<ProjetRecrutement> buildSpecForCurrentUser(
            User user,
            ProjetRecrutementSearchDto search) {

        boolean isDrhOrAdmin = hasRole(user, "DRH") || hasRole(user, "ADMIN");

        if (isDrhOrAdmin) {
            return ProjetRecrutementSpecification.fromSearch(search);
        }

        // DIRECTEUR : filtrer sur ses directions
        List<Long> directionIds = directionRepository.findIdsByDirecteurId(user.getId());
        return ProjetRecrutementSpecification.inDirections(directionIds)
                .and(ProjetRecrutementSpecification.fromSearch(search));
    }

    /**
     * Vérifie que le DIRECTEUR peut accéder à ce projet (sa direction).
     */
    private void assertCanAccess(User user, ProjetRecrutement projet) {
        if (hasRole(user, "DRH") || hasRole(user, "ADMIN")) return;

        List<Long> directionIds = directionRepository.findIdsByDirecteurId(user.getId());
        Long projetDirectionId  = projet.getFicheDePoste().getDirection().getId();

        if (!directionIds.contains(projetDirectionId)) {
            throw new ProjetRecrutementAccessDeniedException(
                    "Accès refusé : ce projet n'appartient pas à vos directions"
            );
        }
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
}
