package com.hrflow.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(AiFallbackProperties.class)
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    private final AiFallbackProperties properties;

    public AiConfig(AiFallbackProperties properties) {
        this.properties = properties;
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    @Bean
    @Primary
    public ChatModel chatModel() {
        List<AiProviderProperties> enabled = properties.enabledProviders();
        List<ChatModel> models = new ArrayList<>(enabled.size());
        List<String>    names  = new ArrayList<>(enabled.size());

        for (AiProviderProperties p : enabled) {
            log.info("Registering sync provider '{}' → {} / {}", p.name(), p.baseUrl(), p.model());
            models.add(buildSyncModel(p));
            names.add(p.name());
        }

        log.info("Sync AI fallback chain: {}", names);
        return new FallbackChatLanguageModel(models, names);
    }


    // ── Factories ─────────────────────────────────────────────────────────────

    private static ChatModel buildSyncModel(AiProviderProperties p) {
        return OpenAiChatModel.builder()
                .baseUrl(p.baseUrl())
                .apiKey(p.apiKey())
                .modelName(p.model())
                .timeout(Duration.ofSeconds(p.timeoutSeconds()))
                .temperature(p.temperature())   // lu depuis la config, plus de valeur codée en dur
                .logRequests(false)
                .logResponses(false)
                .build();
    }

}