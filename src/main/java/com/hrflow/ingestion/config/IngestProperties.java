package com.hrflow.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Configuration de la couche d'ingestion de CV (workflow n8n IMAP + uploads UI).
 *
 * <p>Bound sur le prefix {@code app.ingest}. Lu depuis {@code application.yaml}.
 *
 * @param apiKey       Clé partagée présentée par n8n dans le header {@code X-Ingest-Key}.
 *                     Aucune valeur par défaut : si absente ou vide, le filtre refuse
 *                     tous les appels (fail-secure).
 * @param maxFileSize  Soft cap : taille maximale acceptée pour un CV (PDF/DOCX).
 *                     S'applique aux uploads UI (rejette en 400) ET aux ingests n8n
 *                     (rejette en {@code REJECTED FILE_TOO_LARGE}, 200 OK avec verdict).
 *                     Le hard cap (multipart Spring, voir
 *                     {@code spring.servlet.multipart.max-file-size}) doit être ≥ cette valeur ;
 *                     il existe pour protéger le serveur d'un flux indéfini.
 */
@ConfigurationProperties(prefix = "app.ingest")
public record IngestProperties(
        String   apiKey,
        DataSize maxFileSize
) {}
