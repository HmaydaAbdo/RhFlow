package com.hrflow.ingestion.event;

import com.hrflow.ingestion.model.IngestionSource;

import java.time.LocalDateTime;

/**
 * Publié quand un {@code IngestionRecord} passe en état {@code ERROR}.
 *
 * <p>Erreur <strong>technique</strong> (MinIO indisponible, DB injoignable,
 * etc.) — distincte d'un rejet métier. Ces records sont en théorie retryables.
 *
 * @param recordId    Id du record concerné.
 * @param source      Origine.
 * @param externalId  Identifiant externe.
 * @param detail      Détail technique (tronqué à ~500 caractères côté caller).
 * @param processedAt Timestamp de la transition vers ERROR.
 */
public record IngestionErroredEvent(
        Long             recordId,
        IngestionSource  source,
        String           externalId,
        String           detail,
        LocalDateTime    processedAt
) implements IngestionEvent {}
