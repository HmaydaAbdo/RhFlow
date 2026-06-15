package com.hrflow.ingestion.metrics;

import com.hrflow.ingestion.event.IngestionErroredEvent;
import com.hrflow.ingestion.event.IngestionImportedEvent;
import com.hrflow.ingestion.event.IngestionRejectedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridge entre les événements de domaine de l'ingestion (cf. paquet
 * {@code com.hrflow.ingestion.event}) et le {@link MeterRegistry} de Micrometer.
 *
 * <p>Trois compteurs publiés, tagués pour permettre des découpes dans
 * Prometheus / Grafana :
 *
 * <table>
 *   <caption>Compteurs exposés</caption>
 *   <tr><th>Nom</th>                  <th>Tags</th>                <th>Question répondue</th></tr>
 *   <tr><td>{@code ingestion.imported}</td><td>source</td>          <td>Combien de CVs importés avec succès, par canal ?</td></tr>
 *   <tr><td>{@code ingestion.rejected}</td><td>source, reason</td>  <td>Combien de rejets métier, par canal et par cause typée ?</td></tr>
 *   <tr><td>{@code ingestion.errored}</td> <td>source</td>          <td>Combien d'erreurs techniques (retryables), par canal ?</td></tr>
 * </table>
 *
 * <p>Endpoints d'observation :
 * <ul>
 *   <li>{@code GET /actuator/metrics/ingestion.imported} — JSON, par défaut Spring Boot Actuator</li>
 *   <li>{@code GET /actuator/prometheus} — format Prometheus pour scraping (nécessite la
 *       dépendance {@code micrometer-registry-prometheus} dans le classpath)</li>
 * </ul>
 *
 * <p>Pattern Open-Closed : ajouter une nouvelle dimension d'observation (timer,
 * gauge…) revient à ajouter une nouvelle méthode {@code @EventListener} ici, sans
 * toucher au {@code IngestionRecorder} ni aux services métier.
 */
@Component
public class IngestionMetricsListener {

    private static final String METRIC_IMPORTED = "ingestion.imported";
    private static final String METRIC_REJECTED = "ingestion.rejected";
    private static final String METRIC_ERRORED  = "ingestion.errored";

    private static final String TAG_SOURCE = "source";
    private static final String TAG_REASON = "reason";

    private final MeterRegistry registry;

    public IngestionMetricsListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @EventListener
    public void onImported(IngestionImportedEvent event) {
        Counter.builder(METRIC_IMPORTED)
                .description("Number of CV ingestions that resulted in an IMPORTED record")
                .tag(TAG_SOURCE, event.source().name())
                .register(registry)
                .increment();
    }

    @EventListener
    public void onRejected(IngestionRejectedEvent event) {
        Counter.builder(METRIC_REJECTED)
                .description("Number of CV ingestions that resulted in a REJECTED record")
                .tag(TAG_SOURCE, event.source().name())
                .tag(TAG_REASON, event.reason().name())
                .register(registry)
                .increment();
    }

    @EventListener
    public void onErrored(IngestionErroredEvent event) {
        Counter.builder(METRIC_ERRORED)
                .description("Number of CV ingestions that resulted in an ERROR record (technical, retryable)")
                .tag(TAG_SOURCE, event.source().name())
                .register(registry)
                .increment();
    }
}
