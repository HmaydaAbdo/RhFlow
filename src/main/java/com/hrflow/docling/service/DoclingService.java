package com.hrflow.docling.service;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OcrEngine;
import ai.docling.serve.api.convert.request.options.TableFormerMode;
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
 * Le SDK appelle l'endpoint <b>synchrone</b> {@code POST /v1/convert/source} de
 * docling-serve : la requête bloque jusqu'à la fin de la conversion. La durée
 * totale est bornée par {@code arconia.docling.read-timeout} (120 s actuellement).
 *
 *   Spring Boot ──[URL présignée]──▶ DoclingServeApi ──▶ docling-serve ──▶ MinIO
 *
 * ── Deux modes de conversion ────────────────────────────────────────────────
 *
 * Mode STANDARD (défaut) :
 *   Docling lit la couche texte embarquée dans le PDF — rapide et précis
 *   pour les CVs numériques (Word exporté, LaTeX, etc.).
 *   Images et TableFormer désactivés (inutiles pour extraction texte CV).
 *
 * Mode FORCE OCR (retry) :
 *   Utilisé quand le Markdown standard est trop court (PDF scanné, police
 *   exotique, couche texte corrompue). Docling ignore la couche texte,
 *   rend chaque page en image et passe par EasyOCR (fr + ar).
 *   Plus lent, mais lit visuellement n'importe quel PDF.
 *
 * ── Compatibilité de version ────────────────────────────────────────────────
 *
 * Le couple SDK 0.3.0 / Arconia 0.20.0 est qualifié contre l'image
 * {@code ghcr.io/docling-project/docling-serve-cpu:v1.9.0} (cf. docker-compose).
 * À partir de docling-serve v1.16.x, l'API a été restructurée et l'endpoint sync
 * n'est plus garanti — ne PAS basculer l'image en {@code :latest}.
 *
 * ── Résilience ──────────────────────────────────────────────────────────────
 *
 *  — @CircuitBreaker "docling" : ouvre après 50 % d'échecs sur 10 appels.
 *  — @Retryable × 3 sur erreurs transitoires (réseau, 5xx).
 *    - noRetryFor DoclingConversionException : échec structurel (doc illisible)
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

    /**
     * Options précision maximale — mode standard (couche texte du PDF).
     *
     * Priorité : ne rater aucune information, même dans les sections en tableau.
     *
     * — doOcr=true          : OCR sur les zones bitmap détectées (signatures, tampons…)
     * — forceOcr=false      : la couche texte native d'un PDF numérique est toujours
     *                         plus précise qu'un OCR visuel — on ne la remplace pas.
     * — ocrEngine=EASYOCR   : meilleur moteur pour le français (accents, ligatures)
     * — ocrLang=fr          : ciblé français uniquement — plus précis que multilingual
     * — includeImages=false : les photos de CV ne contiennent pas de texte exploitable
     * — doTableStructure=true : réactivé — beaucoup de CVs structurent compétences,
     *                           langues et expériences en colonnes/tableaux.
     *                           Sans ce modèle, ces sections sont lues en désordre.
     * — tableMode=ACCURATE  : modèle TableFormer complet (vs. fast = heuristique).
     *                         Précision maximale sur les tableaux de compétences.
     */
    private static final ConvertDocumentOptions OPTIONS_STANDARD =
            ConvertDocumentOptions.builder()
                    .doOcr(true)
                    .forceOcr(false)
                    .ocrEngine(OcrEngine.EASYOCR)
                    .ocrLang("fr")
                    .includeImages(false)
                    .doTableStructure(true)
                    .tableMode(TableFormerMode.ACCURATE)
                    .build();

    private final DoclingServeApi doclingClient;

    public DoclingService(DoclingServeApi doclingClient) {
        this.doclingClient = doclingClient;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Convertit un document (PDF ou DOCX) en Markdown — mode standard.
     *
     * Lit la couche texte du PDF directement (rapide, précis pour CVs numériques).
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
        log.debug("[Docling] conversion standard → fichier='{}', url='{}'", filename, documentUrl);
        return convert(documentUrl, filename, OPTIONS_STANDARD, "standard");
    }

    // ── Helper partagé ────────────────────────────────────────────────────────

    private String convert(String documentUrl, String filename,
                           ConvertDocumentOptions options, String mode) {
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
                    .options(options)
                    .build();

            // Endpoint sync : POST /v1/convert/source — bloque jusqu'à la fin
            ConvertDocumentResponse response = doclingClient.convertSource(request);

            String md = extractMarkdown(response, filename);
            log.info("[Docling] '{}' converti en mode {} — {} caractères", filename, mode, md.length());
            return md;

        } catch (DoclingConversionException | CallNotPermittedException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Docling] erreur {}/SDK pour '{}' : {}", mode, filename, e.getMessage());
            throw new DoclingConversionException(
                    "docling-serve inaccessible pour '%s' (mode %s)".formatted(filename, mode), e);
        }
    }

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
        return md;
    }
}
