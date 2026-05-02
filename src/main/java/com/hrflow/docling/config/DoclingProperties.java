package com.hrflow.docling.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriétés de connexion à docling-serve, lues depuis application.yaml.
 *
 * <pre>
 * app:
 *   docling:
 *     base-url: ${DOCLING_URL:http://localhost:5001}
 *     timeout-seconds: 120
 * </pre>
 *
 * @Validated → Spring valide les contraintes au démarrage.
 * Si DOCLING_URL est absent ou vide, l'application refuse de démarrer
 * avec un message clair plutôt qu'un NullPointerException silencieux.
 *
 * Enregistrée via @EnableConfigurationProperties dans DoclingConfig.
 */
@Validated
@ConfigurationProperties(prefix = "app.docling")
public record DoclingProperties(

        /** URL de base du service docling-serve, ex: http://docling-serve:5001 */
        @NotBlank(message = "app.docling.base-url est requis")
        String baseUrl,

        /**
         * Timeout en secondes pour les appels HTTP vers docling-serve.
         * Doit être genereux : la conversion d'un CV dense peut prendre 30–90 s
         * selon la charge du cluster Docling.
         * Valeur recommandée en production : 120 s.
         */
        @Positive(message = "app.docling.timeout-seconds doit être un entier positif")
        int timeoutSeconds

) {}
