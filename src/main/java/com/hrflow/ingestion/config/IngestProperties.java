package com.hrflow.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'endpoint d'ingestion technique (workflow n8n IMAP → backend).
 *
 * <p>Bound sur le prefix {@code app.ingest}. Lu depuis {@code application.yaml}
 * qui pointe sur la variable d'env {@code INGEST_API_KEY} via {@code ${...}}.
 *
 * <p>Pourquoi un record :
 * <ul>
 *   <li>Immuable — pas de risque qu'un bean modifie la clé en cours de route.</li>
 *   <li>Spring Boot 3 le bind nativement comme {@code @ConfigurationProperties}.</li>
 * </ul>
 *
 * @param apiKey Clé partagée présentée par n8n dans le header {@code X-Ingest-Key}.
 *               Aucune valeur par défaut : si absente ou vide, le filtre refusera
 *               tous les appels (fail-secure).
 */
@ConfigurationProperties(prefix = "app.ingest")
public record IngestProperties(
        String apiKey
) {}
