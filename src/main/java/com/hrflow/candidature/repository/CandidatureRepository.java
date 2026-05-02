package com.hrflow.candidature.repository;

import com.hrflow.candidature.model.Candidature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidatureRepository
        extends JpaRepository<Candidature, Long>,
                JpaSpecificationExecutor<Candidature> {

    /**
     * Liste paginée + filtrée des candidatures d'un projet.
     *
     * @EntityGraph précharge projetRecrutement et ficheDePoste en un seul
     * LEFT JOIN — sans in-memory pagination (contrairement à JOIN FETCH + Page).
     *
     * Le tri (score DESC NULLS LAST, deposeLe DESC) est passé dans le Pageable
     * depuis le service via Sort.Order.nullsLast().
     */
    @Override
    @EntityGraph(attributePaths = {
            "projetRecrutement",
            "projetRecrutement.ficheDePoste"
    })
    Page<Candidature> findAll(Specification<Candidature> spec, Pageable pageable);

    /**
     * Vérifie si un email a déjà une autre candidature dans ce projet.
     * IdNot exclut la candidature courante pour éviter l'auto-détection de doublon.
     * IgnoreCase : "Mohamed@Gmail.com" == "mohamed@gmail.com".
     */
    boolean existsByEmailCandidatIgnoreCaseAndProjetRecrutementIdAndIdNot(
            String emailCandidat, Long projetRecrutementId, Long id);

    /**
     * Charge une candidature avec son projet et la fiche de poste en un seul JOIN.
     * JOIN FETCH est sûr ici : on charge une seule entité, pas une collection paginée.
     * Utilisé par le pipeline async et par les endpoints détail / delete / presign.
     */
    @Query("""
        SELECT c FROM Candidature c
        JOIN FETCH c.projetRecrutement pr
        JOIN FETCH pr.ficheDePoste
        WHERE c.id = :id
        """)
    Optional<Candidature> findByIdWithProjet(@Param("id") Long id);

    /** Nombre de CVs pour un projet — utilisé pour le badge UI sans charger les entités. */
    long countByProjetRecrutementId(Long projetId);
}
