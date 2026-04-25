package com.hrflow.direction.repositories;

import com.hrflow.direction.entities.Direction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DirectionRepository extends JpaRepository<Direction, Long>,
        JpaSpecificationExecutor<Direction> {

    boolean existsByNom(String nom);

    boolean existsByNomAndIdNot(String nom, Long id);

    /** Fetch paginé avec directeur — évite N+1 dans search(). */
    @EntityGraph(attributePaths = "directeur")
    @Override
    Page<Direction> findAll(Specification<Direction> spec, Pageable pageable);

    @EntityGraph(attributePaths = "directeur")
    Optional<Direction> findWithDirecteurById(Long id);

    /** Count unique pour un seul id (findById). */
    @Query("SELECT COUNT(f) FROM FicheDePoste f WHERE f.direction.id = :directionId")
    long countFichesDePoste(Long directionId);

    /** Bulk count — une seule requête pour toute la page. Retourne [directionId, count]. */
    @Query("SELECT f.direction.id, COUNT(f) FROM FicheDePoste f WHERE f.direction.id IN :directionIds GROUP BY f.direction.id")
    List<Object[]> countFichesDePosteByDirectionIds(Collection<Long> directionIds);

    // ---- Directions gérées par un directeur donné ----
    @Query("SELECT d.id FROM Direction d WHERE d.directeur.id = :directeurId")
    List<Long> findIdsByDirecteurId(Long directeurId);

    List<Direction> findByDirecteurId(Long directeurId);
}
