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
 * @param rateLimit    Paramètres du token bucket appliqué à {@code POST /ingest/cv}.
 */
@ConfigurationProperties(prefix = "app.ingest")
public record IngestProperties(
        String    apiKey,
        DataSize  maxFileSize,
        RateLimit rateLimit
) {

    /**
     * Sous-config du token bucket (algo Bucket4j).
     *
     * @param capacity        Taille max du réservoir = burst autorisé.
     *                        Ex. 100 : on peut envoyer 100 requêtes d'un coup.
     * @param refillPerMinute Nombre de jetons réinjectés par minute (taux moyen).
     *                        Ex. 100 : sur la durée, max 100 req/min.
     */
    public record RateLimit(
            int capacity,
            int refillPerMinute
    ) {}
}
