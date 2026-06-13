package com.hrflow.ingestion.repositories;

import com.hrflow.ingestion.model.IngestionRecord;
import com.hrflow.ingestion.model.IngestionSource;
import com.hrflow.ingestion.model.IngestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngestionRecordRepository extends JpaRepository<IngestionRecord, Long> {

    /**
     * Lookup d'idempotence : retrouve un record par sa clé naturelle externe.
     * Utilisé en première ligne par {@code IngestionService.ingest()} pour
     * détecter qu'un email a déjà été traité (même Message-ID + même source).
     */
    Optional<IngestionRecord> findByExternalIdAndSource(String externalId, IngestionSource source);

    /**
     * Liste paginée pour la « Boîte de réception » DRH, filtrable par statut.
     * Préchargement de la candidature liée pour l'UI (lazy par défaut sinon).
     */
    @EntityGraph(attributePaths = {"candidature", "candidature.projetRecrutement"})
    Page<IngestionRecord> findByStatus(IngestionStatus status, Pageable pageable);

    /**
     * Liste paginée sans filtre — onglet "Tous" de l'UI.
     */
    @EntityGraph(attributePaths = {"candidature", "candidature.projetRecrutement"})
    @Override
    Page<IngestionRecord> findAll(Pageable pageable);

    /**
     * Détail enrichi pour la page de re-routage (rattacher à un projet).
     */
    @EntityGraph(attributePaths = {"candidature", "candidature.projetRecrutement"})
    Optional<IngestionRecord> findWithDetailsById(Long id);
}
