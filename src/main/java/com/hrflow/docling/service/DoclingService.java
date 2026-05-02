package com.hrflow.docling.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hrflow.docling.config.DoclingProperties;
import com.hrflow.docling.exception.DoclingConversionException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Client HTTP vers le microservice docling-serve.
 *
 * docling-serve convertit des documents (PDF, DOCX, …) en Markdown structuré
 * grâce à un pipeline IA (layout analysis, table extraction, OCR si nécessaire).
 * Il fonctionne comme un service indépendant scalable (Docker / Kubernetes).
 *
 * ── Architecture URL-source (recommandée en production) ───────────────────────
 *
 * Ce service envoie une URL présignée MinIO à docling-serve.
 * docling-serve télécharge le fichier DIRECTEMENT depuis MinIO, sans que
 * Spring Boot charge les bytes en mémoire ni les Base64-encode.
 *
 *   Spring Boot ──[URL]──▶ docling-serve ──[GET]──▶ MinIO
 *                                        ◀─[bytes]─
 *
 * Avantages vs l'approche Base64 :
 *  — Zéro copie mémoire dans le thread Spring Boot
 *  — Pas d'overhead Base64 (+33% sur la taille du payload)
 *  — docling-serve télécharge en streaming natif, pas de buffering supplémentaire
 *
 * Prérequis réseau : docling-serve doit pouvoir joindre MinIO.
 * En Docker Compose / Kubernetes, configurer app.minio.endpoint avec le hostname
 * interne du service (ex: http://minio:9000), pas localhost.
 *
 * Endpoint : POST {baseUrl}/v1/convert/source  (API stable v1, format v1.9+)
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

    /** Endpoint de conversion stable (API v1, format sources unifié v1.9+). */
    private static final String CONVERT_PATH = "/v1/convert/source";

    /** Endpoint de santé docling-serve — vérifié au démarrage Spring. */
    private static final String HEALTH_PATH = "/health";

    /** Kind HTTP utilisé dans le payload sources (API docling-serve v1.9+). */
    private static final String KIND_HTTP = "http";

    private final RestClient        restClient;
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
     * Convertit un document (PDF ou DOCX) en Markdown via docling-serve.
     *
     * Le document est téléchargé directement depuis {@code documentUrl} par docling-serve —
     * Spring Boot ne charge aucun byte en mémoire. Fournir une URL présignée MinIO
     * valide (générée via {@code MinioService.presignedUrl()}).
     *
     * Prérequis : docling-serve doit pouvoir résoudre l'URL (accès réseau à MinIO).
     * En Docker Compose / Kubernetes, utiliser le hostname interne pour app.minio.endpoint.
     *
     * Retry : 3 tentatives, backoff exponentiel 2 s → 4 s.
     * Couvre les erreurs transitoires (timeout réseau, restart docling-serve).
     * Les {@link DoclingConversionException} (réponse vide, Markdown vide) ne sont PAS
     * retriées — elles indiquent un problème structurel (PDF scanné, fichier corrompu).
     *
     * @param documentUrl URL accessible par docling-serve (ex: URL présignée MinIO interne)
     * @param filename    Nom du fichier, utilisé pour les logs (ex: "cv_alami.pdf")
     * @return            Contenu Markdown structuré extrait du document
     * @throws DoclingConversionException si docling-serve est indisponible, si la réponse
     *                                    est vide, ou si l'URL n'est pas joignable par docling
     */
    @Retryable(
        retryFor   = RestClientException.class,
        maxAttempts = 3,
        backoff    = @Backoff(delay = 2000, multiplie