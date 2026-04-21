package com.hrflow.besoinrecrutement.repositories;

import com.hrflow.besoinrecrutement.model.BesoinRecrutement;
import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.model.StatutBesoin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BesoinRecrutementRepository
        extends JpaRepository<BesoinRecrutement, Long>,
                JpaSpecificationExecutor<BesoinRecrutement> {

    // ---- Fetch complet pour GET by ID ----
    @EntityGraph(attributePaths = {"ficheDePoste", "ficheDePoste.direction", "directeur"})
    Optional<BesoinRecrutement> findWithDetailsById(Long id);

    // ---- Unicité : un seul besoin EN_COURS par fiche de poste ----
    boolean existsByFicheDePosteIdAndStatut(Long ficheDePosteId, StatutBesoin statut);

    // ---- Unicité en excluant l'entité courante (update) ----
    boolean existsByFicheDePosteIdAndStatutAndIdNot(Long ficheDePosteId, StatutBesoin statut, Long id);

    // ---- Stats globales par statut ----
    long countByStatut(StatutBesoin statut);

    // ---- Stats par priorité ----
    @Query("""
        SELECT b.priorite, COUNT(b)
        FROM BesoinRecrutement b
        GROUP BY b.priorite
        """)
    List<Object[]> countGroupByPriorite();

    // ---- Stats : count par (direction, statut) — agrégé dans le service ----
    @Query("""
        SELECT d.id, d.nom, b.statut, COUNT(b)
        FROM BesoinRecrutement b
        JOIN b.ficheDePoste f
        JOIN f.direction d
        GROUP BY d.id, d.nom, b.statut
        ORDER BY d.nom
        """)
    List<Object[]> countGroupByDirectionAndStatut();
}
