package com.hrflow.direction.repositories;

import com.hrflow.direction.entities.Direction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectionRepository extends JpaRepository<Direction, Long>,
        JpaSpecificationExecutor<Direction> {

    boolean existsByNom(String nom);

    boolean existsByNomAndIdNot(String nom, Long id);

    @EntityGraph(attributePaths = "directeur")
    Optional<Direction> findWithDirecteurById(Long id);

    @Query("SELECT COUNT(f) FROM FicheDePoste f WHERE f.direction.id = :directionId")
    long countFichesDePoste(Long directionId);

    // ---- Directions gérées par un directeur donné ----
    @Query("SELECT d.id FROM Direction d WHERE d.directeur.id = :directeurId")
    List<Long> findIdsByDirecteurId(Long directeurId);

    List<Direction> findByDirecteurId(Long directeurId);
}
