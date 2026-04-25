package com.hrflow.fichedeposte.repositories;

import com.hrflow.fichedeposte.model.FicheDePoste;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FicheDePosteRepository extends JpaRepository<FicheDePoste, Long>,
        JpaSpecificationExecutor<FicheDePoste> {

    /** Fetch paginé avec direction — évite N+1 dans search(). */
    @EntityGraph(attributePaths = "direction")
    @Override
    Page<FicheDePoste> findAll(Specification<FicheDePoste> spec, Pageable pageable);

    @EntityGraph(attributePaths = "direction")
    Optional<FicheDePoste> findWithDirectionById(Long id);

    @EntityGraph(attributePaths = {"direction", "direction.directeur"})
    Optional<FicheDePoste> findWithDirectionAndDirecteurById(Long id);

    @EntityGraph(attributePaths = "direction")
    @Query("SELECT f FROM FicheDePoste f WHERE f.direction.id = :directionId")
    Page<FicheDePoste> findByDirectionId(Long directionId, Pageable pageable);

    @EntityGraph(attributePaths = {"direction", "direction.directeur"})
    @Query("SELECT f FROM FicheDePoste f")
    List<FicheDePoste> findAllWithDirectionAndDirecteur();
}
