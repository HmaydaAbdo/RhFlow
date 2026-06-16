package com.hrflow.ingestion.dto;

import com.hrflow.ingestion.model.IngestionRejectionReason;
import com.hrflow.ingestion.model.IngestionSource;
import com.hrflow.ingestion.model.IngestionStatus;

import java.time.LocalDateTime;

/**
 * Représentation publique d'un {@code IngestionRecord} pour l'API.
 *
 * <p>Sert deux usages :
 * <ul>
 *   <li><b>Réponse à n8n</b> après un POST {@code /ingest/cv} — n8n y voit
 *       le verdict (importé/rejeté/erreur) et la raison.</li>
 *   <li><b>Liste « CV reçus »</b> dans la « Boîte de réception » DRH.</li>
 * </ul>
 *
 * <p>Ce que ce DTO ne contient PAS :
 * <ul>
 *   <li>{@code version} (optimistic locking — interne Hibernate)</li>
 *   <li>L'entité {@code Candidature} en entier (juste son id en {@link #candidatureId})</li>
 * </ul>
 *
 * @param id              Id technique du record (sert au frontend pour les actions ciblées).
 * @param source          D'où vient ce CV (EMAIL pour n8n IMAP).
 * @param externalId      Identifiant fourni par la source — Message-ID pour les emails.
 * @param referenceCode   Code projet extrait du sujet (ex. {@code BES-001}), null si absent.
 * @param nomFichier      Nom de la pièce jointe (avant slugify MinIO).
 * @param rawMetadata     JSON brut (expéditeur, sujet complet…) — pour audit DRH.
 * @param status          État courant du traitement.
 * @param rejectionReason Renseignée uniquement si {@code status == REJECTED}.
 * @param rejectionDetail Texte libre complétant la raison (ex. code attendu vs reçu).
 * @param candidatureId        Renseigné uniquement si {@code status == IMPORTED}.
 * @param projetRecrutementId  Id du projet de recrutement lié — permet à l'UI
 *                             DRH de naviguer depuis la « Boîte de réception »
 *                             vers la page de la candidature
 *                             ({@code /projets-recrutement/:projetId/candidatures/:id}).
 *                             Null si pas de candidature liée (rejet/erreur).
 * @param receivedAt      Quand le record a été créé en base.
 * @param processedAt     Quand le statut final a été posé (null si encore PENDING).
 */
public record IngestionRecordResponse(
        Long                      id,
        IngestionSource           source,
        String                    externalId,
        String                    referenceCode,
        String                    nomFichier,
        String                    rawMetadata,
        IngestionStatus           status,
        IngestionRejectionReason  rejectionReason,
        String                    rejectionDetail,
        Long                      candidatureId,
        Long                      projetRecrutementId,
        LocalDateTime             receivedAt,
        LocalDateTime             processedAt
) {}
