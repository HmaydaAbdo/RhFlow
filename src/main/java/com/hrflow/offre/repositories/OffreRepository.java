package com.hrflow.offre.repositories;

import com.hrflow.offre.model.Offre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OffreRepository extends JpaRepository<Offre, Long> {

    /** Récupère l'offre d'un projet, avec son projet chargé eagerly. */
    @Query("""
        SELECT o FROM Offre o
        JOIN FETCH o.projetRecrutement pr
        WHERE pr.id = :projetId
    """)
    Optional<Offre> findByProjetRecrutementId(@Param("projetId") Long projetId);

    boolean existsByProjetRecrutementId(Long projetId);
}
