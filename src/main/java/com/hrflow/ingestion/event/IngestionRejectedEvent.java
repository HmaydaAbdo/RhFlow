package com.hrflow.ingestion.event;

import com.hrflow.ingestion.model.IngestionRejectionReason;
import com.hrflow.ingestion.model.IngestionSource;

import java.time.LocalDateTime;

/**
 * Publié quand un {@code IngestionRecord} passe en état {@code REJECTED}.
 *
 * <p>Rejet <strong>métier</strong> — pas une erreur technique. La raison est
 * typée ({@code IngestionRejectionReason}) pour faciliter le filtrage et le
 * comptage côté listener (métriques, alertes…).
 *
 * @param recordId    Id du record concerné.
 * @param source      Origine.
 * @param externalId  Identifiant externe.
 * @param reason      Cause métier typée (NO_REFERENCE_CODE, PROJECT_CLOSED…).
 * @param detail      Texte libre complétant la raison (peut être null).
 * @param processedAt Timestamp de la transition vers REJECTED.
 */
public record IngestionRejectedEvent(
        Long                      recordId,
        IngestionSource           source,
        String                    externalId,
        IngestionRejectionReason  reason,
        String                    detail,
        LocalDateTime             processedAt
) implements IngestionEvent {}
