package com.hrflow.ingestion.logging;

import org.slf4j.MDC;

/**
 * Clés MDC (Mapped Diagnostic Context) injectées dans les logs du flux ingestion.
 *
 * <p>Permet à un opérateur de retrouver TOUTES les lignes de log liées à un même
 * CV avec une commande shell :
 *
 * <pre>{@code
 * grep "msg-id-abc" app.log
 * grep "rec=42" app.log
 * }</pre>
 *
 * <p>Convention de nommage : préfixe {@code ingest.} pour éviter les collisions
 * avec d'autres modules qui utiliseraient MDC. Mise à jour cohérente avec la
 * config {@code logging.pattern.console} dans {@code application.yaml}.
 *
 * <p>Pattern d'usage (controller / service de haut niveau) :
 *
 * <pre>{@code
 * try {
 *     MDC.put(IngestMdc.SOURCE, source.name());
 *     MDC.put(IngestMdc.EXTERNAL_ID, externalId);
 *     return service.doWork(...);
 * } finally {
 *     IngestMdc.clear();
 * }
 * }</pre>
 *
 * <p>Limite connue : MDC est <strong>thread-local</strong>. Sur un appel
 * {@code @Async} (ex : {@code CvPipelineService.traiter()}), les valeurs ne
 * sont PAS propagées automatiquement. Le pipeline IA gère son propre contexte
 * (id candidature) — la jonction se fait via la table {@code ingestion_records}.
 */
public final class IngestMdc {

    /** Source d'ingestion (EMAIL, MANUAL_UI…). */
    public static final String SOURCE      = "ingest.source";

    /** Identifiant externe (Message-ID email, ou {@code manual-{UUID}}). */
    public static final String EXTERNAL_ID = "ingest.externalId";

    /** Id du IngestionRecord créé (renseigné seulement après {@code createPending}). */
    public static final String RECORD_ID   = "ingest.recordId";

    private IngestMdc() {}

    /** Met les 3 clés en une fois — typique côté controller à l'entrée. */
    public static void put(String source, String externalId, String recordId) {
        if (source     != null) MDC.put(SOURCE,      source);
        if (externalId != null) MDC.put(EXTERNAL_ID, externalId);
        if (recordId   != null) MDC.put(RECORD_ID,   recordId);
    }

    /** Pose juste le recordId — typiquement appelé en plein milieu du flux. */
    public static void putRecordId(long recordId) {
        MDC.put(RECORD_ID, String.valueOf(recordId));
    }

    /** Nettoyage à appeler dans le {@code finally} du controller/service de haut niveau. */
    public static void clear() {
        MDC.remove(SOURCE);
        MDC.remove(EXTERNAL_ID);
        MDC.remove(RECORD_ID);
    }
}
