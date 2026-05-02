package com.hrflow.docling.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hrflow.docling.config.DoclingProperties;
import com.hrflow.docling.exception.DoclingConversionException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;

/**
 * Client HTTP vers le microservice docling-serve.
 *
 * docling-serve convertit des documents (PDF, DOCX, …) en Markdown structuré
 * grâce à un pipeline IA (layout analysis, table extraction, OCR si nécessaire).
 * Il fonctionne comme un service indépendant scalable (Docker / Kubernetes).
 *
 * Endpoint : POST {baseUrl}/v1/convert/source   (API stable, v1alpha obsolète)
 *
 * Déploiement local :
 *   docker run -p 5001:5001 ds4sd/docling-serve:latest
 *
 * Toutes les erreurs sont remontées sous forme de {@link DoclingConversionException}
 * (unchecked), traitée par GlobalExceptionHandler → 503 Service Unavailable.
 */
@Service
public class DoclingService {

    private static final Logger log = LoggerFactory.getLogger(DoclingService.class);

    /** Endpoint de conversion (API stable docling-serve ≥ 1.0). */
    private static final String CONVERT_PATH = "/v1/convert/source";

    /** Endpoint de santé docling-serve — vérifié au démarrage Spring. */
    private static final String HEALTH_PATH = "/health";

    private final RestClient       restClient;
    private final DoclingProperties props;

    public DoclingService(@Qualifier("doclingRestClient") RestClient restClient,
                          DoclingProperties props) {
        this.restClient = restClient;
        this.props      = props;
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Vérifie la disponibilité de docling-serve au démarrage de l'application.
     * Fail-open : si docling-serve est down, un warning est loggé mais l'app démarre.
     * L'erreur se révèlera au premier upload de CV.
     */
    @PostConstruct
    void init() {
        try {
            restClient.get()
                    .uri(HEALTH_PATH)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Docling] service disponible à {}", props.baseUrl());
        } catch (Exception e) {
            log.warn("[Docling] service non joignable au démarrage ({}) : {} — " +
                     "vérifiez que docling-serve est lancé.", props.baseUrl(), e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Convertit un fichier (PDF ou DOCX) en Markdown via docling-serve.
     *
     * Le fichier est lu en mémoire depuis l'InputStream, encodé en Base64,
     * puis envoyé à docling-serve qui applique son pipeline IA de conversion
     * (layout analysis, tables, OCR si nécessaire).
     *
     * @param fileStream  InputStream du fichier (depuis MinIO — DOIT être fermé par l'appelant)
     * @param filename    Nom original du fichier, ex: "cv_alami.pdf" (docling infère le format)
     * @return            Contenu Markdown structuré extrait du document
     * @throws DoclingConversionException si docling-serve est indisponible, si la réponse
     *                                    est vide, ou si la lecture du stream échoue
     */
    public String toMarkdown(InputStream fileStream, String filename) {
        byte[] fileBytes = readAllBytes(fileStream, filename);
        String base64    = Base64.getEncoder().encodeToString(fileBytes);

        ConvertRequest request = buildRequest(base64, filename);

        log.debug("[Docling] POST {} — fichier='{}' ({} bytes)", CONVERT_PATH, filename, fileBytes.length);

        try {
            ConvertResponse response = restClient.post()
                    .uri(CONVERT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ConvertResponse.class);

            return extractMarkdown(response, filename);

        } catch (RestClientException e) {
            log.error("[Docling] échec conversion '{}' : {}", filename, e.getMessage());
            throw new DoclingConversionException(
                    "docling-serve inaccessible ou conversion échouée pour '%s'".formatted(filename), e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ConvertRequest buildRequest(String base64, String filename) {
        var fileSource = new FileSource(base64, filename);
        var options    = new ConvertOptions(List.of("md"));
        return new ConvertRequest(List.of(fileSource), options);
    }

    private String extractMarkdown(ConvertResponse response, String filename) {
        if (response == null || response.document() == null) {
            throw new DoclingConversionException(
                    "Réponse docling-serve vide pour '%s'".formatted(filename));
        }
        String md = response.document().mdContent();
        if (md == null || md.isBlank()) {
            throw new DoclingConversionException(
                    "Markdown vide retourné par docling-serve pour '%s'".formatted(filename));
        }
        log.info("[Docling] '{}' converti — {} caractères Markdown", filename, md.length());
        return md;
    }

    private byte[] readAllBytes(InputStream is, String filename) {
        try {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new DoclingConversionException(
                    "Impossible de lire le fichier '%s' depuis le stream".formatted(filename), e);
        }
    }

    // ── Request / Response DTOs (internes à ce service) ──────────────────────

    record FileSource(
            @JsonProperty("base64_string") String base64String,
            @JsonProperty("filename")      String filename
    ) {}

    record ConvertOptions(
            @JsonProperty("to_formats") List<String> toFormats
    ) {}

    record ConvertRequest(
            @JsonProperty("file_sources") List<FileSource>  fileSources,
            @JsonProperty("options")      ConvertOptions     options
    ) {}

    record DocumentContent(
            @JsonProperty("md_content") String mdContent
    ) {}

    record ConvertResponse(
            @JsonProperty("document") DocumentContent document
    ) {}
}
