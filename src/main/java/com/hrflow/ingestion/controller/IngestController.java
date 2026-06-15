package com.hrflow.ingestion.controller;

import com.hrflow.ingestion.dto.IngestionRecordResponse;
import com.hrflow.ingestion.logging.IngestMdc;
import com.hrflow.ingestion.model.IngestionSource;
import com.hrflow.ingestion.service.IngestionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint technique d'ingestion de CV externe.
 *
 * <p>Source unique pour l'instant : workflow n8n IMAP → POST /ingest/cv.
 * Demain on pourra ajouter d'autres canaux (Slack bot, formulaire carrière, etc.)
 * en passant simplement une {@code source} différente — la logique métier dans
 * {@code IngestionService} est source-agnostique.
 *
 * <p>Sécurité (défense en profondeur) :
 * <ol>
 *   <li>{@code IngestApiKeyFilter} vérifie le header {@code X-Ingest-Key} et pose
 *       une authentication synthétique avec l'authority {@code INGEST}.</li>
 *   <li>{@code @PreAuthorize("hasAuthority('INGEST')")} sur l'endpoint refuse
 *       si l'authority n'est pas posée — au cas où la config Security est mal
 *       câblée, le filtre désactivé, etc.</li>
 * </ol>
 *
 * <p>Contrat HTTP :
 * <ul>
 *   <li><b>200 OK</b> — toujours, même pour les rejets métier (UNKNOWN_REFERENCE,
 *       PROJECT_CLOSED, FILE_TOO_LARGE…). Le body porte le verdict via
 *       {@code status} et {@code rejectionReason}. n8n ne doit pas retry sur 200.</li>
 *   <li><b>400 Bad Request</b> — paramètre manquant ou malformé (Spring auto-gérée).</li>
 *   <li><b>401 Unauthorized</b> — header {@code X-Ingest-Key} manquant ou invalide
 *       (renvoyé par {@code IngestApiKeyFilter}).</li>
 *   <li><b>5xx</b> — erreur technique inattendue (le service essaie de toujours
 *       marquer le record ERROR et renvoyer 200 ; les 5xx sont des cas extrêmes).</li>
 * </ul>
 *
 * <p>Format de la requête (multipart/form-data) :
 * <pre>
 *   file          : la pièce jointe PDF/DOCX (requis)
 *   externalId    : Message-ID de l'email (requis, sert d'idempotence)
 *   source        : IngestionSource — typiquement "EMAIL" (requis)
 *   referenceCode : code projet extrait du sujet, ex. "BES-001" (optionnel)
 *   rawMetadata   : JSON brut (expéditeur, sujet…) pour audit DRH (optionnel)
 * </pre>
 */
@RestController
@RequestMapping("/ingest")
public class IngestController {

    private final IngestionService ingestionService;

    public IngestController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PreAuthorize("hasAuthority('INGEST')")
    @PostMapping("/cv")
    public IngestionRecordResponse ingestCv(
            @RequestParam("file")                                MultipartFile  file,
            @RequestParam("externalId")                          String         externalId,
            @RequestParam("source")                              IngestionSource source,
            @RequestParam(value = "referenceCode", required = false) String     referenceCode,
            @RequestParam(value = "rawMetadata",   required = false) String     rawMetadata) {

        // Garde-fous explicites — Spring déclencherait MissingServletRequestParameter
        // pour les requis manquants (→ 400 auto), mais on veut un message clair sur les
        // champs vides.
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("Le paramètre 'externalId' est obligatoire.");
        }

        // MDC pour la traçabilité : tous les logs du flux (controller + service +
        // recorder) porteront source et externalId. Le recordId sera ajouté en cours
        // de route par IngestionService.ingest() après createPending.
        try {
            IngestMdc.put(source.name(), externalId, null);
            return ingestionService.ingest(file, source, externalId, referenceCode, rawMetadata);
        } finally {
            IngestMdc.clear();
        }
    }
}
