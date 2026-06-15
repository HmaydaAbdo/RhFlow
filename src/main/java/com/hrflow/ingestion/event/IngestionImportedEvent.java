package com.hrflow.ingestion.event;

import com.hrflow.ingestion.model.IngestionSource;

import java.time.LocalDateTime;

/**
 * Publié quand un {@code IngestionRecord} passe en état {@code IMPORTED}.
 *
 * <p>Porteur de l'id de la candidature créée → un listener peut faire des actions
 * post-import (envoyer un accusé de réception à un système externe, etc.).
 *
 * @param recordId      Id du record d'ingestion concerné.
 * @param source        Origine (EMAIL, MANUAL_UI…).
 * @param externalId    Identifiant côté source (Message-ID ou "manual-{UUID}").
 * @param candidatureId Id de la candidature créée par {@code createCandidature}.
 * @param processedAt   Timestamp de la transition vers IMPORTED.
 */
public record IngestionImportedEvent(
        Long             recordId,
        IngestionSource  source,
        String           externalId,
        Long             candidatureId,
        LocalDateTime    processedAt
) implements IngestionEvent {}
