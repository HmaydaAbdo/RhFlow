package com.hrflow.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration unique de la couche IA.
 *
 * <p>Un seul {@link ChatModel} bean : la chaîne de fallback multi-providers
 * ({@link FallbackChatLanguageModel}). Tous les services IA annotés
 * {@code @AiService} (CvDataExtractor, CvEvaluator, JobAnnouncementGenerator)
 * sont câblés dessus automatiquement par l'autoconfig LangChain4j Spring.
 *
 * <p>La température est lue par-provider depuis {@code app.ai.providers[].temperature}
 * dans {@code application.yaml} — actuellement {@code 0.0} pour tous les usages
 * (extraction, évaluation, génération). À l'avenir, quand le projet aura son
 * propre infra et un seul modèle, la spécialisation par usage se fera côté
 * {@code @AiService} en acceptant {@code ChatRequest} pour spécifier les
 * paramètres au call site.
 */
@Configuration
@EnableConfigurationProperties(AiFallbackProperties.class)
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    private final AiFallbackProperties properties;

    public AiConfig(AiFallbackProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ChatModel chatModel() {
        List<AiProviderProperties> enabled    = properties.enabledProviders();
        boolean                    debugLogs  = properties.debugLogs();
        List<ChatModel>            models     = new ArrayList<>(enabled.size());
        List<String>               names      = new ArrayList<>(enabled.size());

        for (AiProviderProperties p : enabled) {
            log.info("Registering sync provider '{}' → {} / {}", p.name(), p.baseUrl(), p.model());
            models.add(buildSyncModel(p, debugLogs));
            names.add(p.name());
        }

        if (debugLogs) {
            log.warn("AI debug logs ENABLED — full CV content will be written to application logs (PII risk)");
        }
        log.info("Sync AI fallback chain: {}", names);
        return new FallbackChatLanguageModel(models, names);
    }

    private static ChatModel buildSyncModel(AiProviderProperties p, boolean debugLogs) {
        return OpenAiChatModel.builder()
                .baseUrl(p.baseUrl())
                .apiKey(p.apiKey())
                .modelName(p.model())
                .timeout(Duration.ofSeconds(p.timeoutSeconds()))
                .temperature(p.temperature())
                .logRequests(debugLogs)
                .logResponses(debugLogs)
                .build();
    }
}
