package com.hrflow.docling.service;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import com.hrflow.docling.exception.DoclingConversionException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Client Docling basé sur le SDK officiel (docling-serve-api 0.3.0 via arconia-bom 0.20.0).
 *
 * ── Architecture ────────────────────────────────────────────────────────────
 *
 * Ce service délègue à {@link DoclingServeApi} auto-configuré par Arconia.
 * Le SDK gère en interne le protocole async de docling-serve (soumission → polling → résultat).
 *
 *   Spring Boot ──[URL présignée]──▶ DoclingServeApi ──▶ docling-serve ──▶ MinIO
 *
 * ── Résilience ──────────────────────────────────────────────────────────────
 *
 *  — @CircuitBreaker "docling" : ouvre après 50 % d'échecs sur 10 appels.
 *  — @Retryable × 3 sur erreurs transitoires.
 *    - noRetryFor DoclingConversionException : échec structurel (doc illisible, markdown vide)
 *    - noRetryFor CallNotPermittedException  : CB ouvert, inutile de retenter
 *
 * ── Configuration ───────────────────────────────────────────────────────────
 *
 *   arconia.docling.base-url        = ${DOCLING_URL:http://localhost:5001}
 *   arconia.docling.read-timeout    = 120s
 *   arconia.docling.connect-timeout = 5s
 */
@Service
public class DoclingService {

    private static final Logger log = LoggerFactory.getLogger(DoclingService.class);

    private final DoclingServeApi doclingClient;

    public DoclingService(DoclingServeApi doclingClient) {
        this.doclingClient = doclingClient;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Convertit un document (PDF ou DOCX) en Markdown via docling-serve.
     *
     * Transmet une URL présignée MinIO — docling-serve télécharge le fichier directement.
     * Spring Boot ne charge aucun byte en mémoire : zéro copie.
     *
     * @param documentUrl URL présignée MinIO accessible par docling-serve
     * @param filename    Nom du fichier (logs uniquement)
     * @return Contenu Markdown extrait du document
     * @throws DoclingConversionException si docling-serve échoue ou Markdown vide
     */
    @CircuitBreaker(name = "docling")
    @Retryable(
        retryFor   = Exception.class,
        noRetryFor = {CallNotPermittedException.class, DoclingConversionException.class},
        maxAttempts = 3,
        backoff     = @Backoff(delay = 2000, multiplier = 2, maxDelay = 8000)
    )
    public String toMarkdown(String documentUrl, String filename) {
        log.debug("[Docling] conversion → fichier='{}', url='{}'", filename, documentUrl);

        try {
            URI uri;
            try {
                uri = new URI(documentUrl);
            } catch (URISyntaxException e) {
                throw new DoclingConversionException(
                        "URL présignée invalide pour '%s' : %s".formatted(filename, e.getMessage()));
            }

            ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                    .source(HttpSource.builder().url(uri).build())
                    .build();

            // SDK gère l'async interne : submit → poll → result — un seul appel bloquant.
            ConvertDocumentResponse response = doclingClient.convertSource(request);

            return extractMarkdown(response, filename);

        } catch (DoclingConversionException | CallNotPermittedException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Docling] erreur réseau/SDK pour '{}' : {}", filename, e.getMessage());
            throw new DoclingConversionException(
                    "docling-serve inaccessible pour '%s'".formatted(filename), e);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String extractMarkdown(ConvertDocumentResponse response, String filename) {
        if (response == null || response.getDocument() == null) {
            throw new DoclingConversionException(
                    "Réponse docling-serve vide pour '%s'".formatted(filename));
        }
        String md = response.getDocument().getMarkdownContent();
        if (md == null || md.isBlank()) {
            throw new DoclingConversionException(
                    "Markdown vide retourné par docling-serve pour '%s'".formatted(filename));
        }
        log.info("[Docling] '{}' converti — {} caractères Markdown", filename, md.length());
        return md;
    }
}
