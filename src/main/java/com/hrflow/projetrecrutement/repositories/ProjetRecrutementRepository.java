package com.hrflow.projetrecrutement.repositories;

import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import com.hrflow.projetrecrutement.model.StatutProjet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProjetRecrutementRepository
        extends JpaRepository<ProjetRecrutement, Long>,
                JpaSpecificationExecutor<ProjetRecrutement> {

    @EntityGraph(attributePaths = {
        "ficheDePoste",
        "ficheDePoste.direction",
        "ficheDePoste.direction.directeur",
        "besoinRecrutement",
        "besoinRecrutement.directeur"
    })
    Optional<ProjetRecrutement> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {
        "ficheDePoste",
        "ficheDePoste.direction",
        "ficheDePoste.direction.directeur"
    })
    @Override
    Page<ProjetRecrutement> findAll(Specification<ProjetRecrutement> spec, Pageable pageable);

    boolean existsByBesoinRecrutementId(Long besoinId);

    /** Vérifie l'unicité de l'objet candidature en excluant le projet en cours de modification. */
    boolean existsByObjetCandidatureIgnoreCaseAndIdNot(String objetCandidature, Long id);

    long countByStatut(StatutProjet statut);

    @Query("""
        SELECT p FROM ProjetRecrutement p
        JOIN FETCH p.ficheDePoste f
        JOIN FETCH f.direction d
        JOIN FETCH d.directeur
        WHERE p.besoinRecrutement.id = :besoinId
    """)
    Optional<ProjetRecrutement> findByBesoinRecrutementId(Long besoinId);
}
