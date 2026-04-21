package com.hrflow.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Top-level configuration properties bound from application.yaml under
 * the "app.ai" prefix.
 *
 * Example YAML:
 * <pre>
 * app:
 *   ai:
 *     providers:
 *       - name: groq
 *         base-url: https://api.groq.com/openai/v1
 *         api-key: ${GROQ_API_KEY}
 *         model: llama-3.3-70b-versatile
 *         timeout-seconds: 20
 *         enabled: true
 *       - name: ollama
 *         base-url: http://localhost:11434/v1
 *         api-key: ollama
 *         model: qwen3:7b
 *         timeout-seconds: 120
 *         enabled: true
 * </pre>
 *
 * Providers are tried in declaration order. The first successful response wins.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiFallbackProperties(
        List<AiProviderProperties> providers,
        CompanyProfile company
) {

    public AiFallbackProperties {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException(
                    "At least one AI provider must be configured under 'app.ai.providers'");
        }
        if (company == null) {
            throw new IllegalStateException(
                    "Company profile must be configured under 'app.ai.company'");
        }
    }

    /** Returns only the enabled providers, in declaration order. */
    public List<AiProviderProperties> enabledProviders() {
        return providers.stream()
                .filter(AiProviderProperties::enabled)
                .toList();
    }

    /**
     * Identifies the company on whose behalf job offers are generated.
     * All fields are injected from application.yaml — no hardcoded values in code.
     *
     * @param nom               Legal name (e.g. "STAPORT SA")
     * @param secteur           Industry sector (e.g. "BTP")
     * @param ville             City / country (e.g. "Casablanca, Maroc")
     * @param anneeFondation    Year founded — used to compute seniority in copy
     * @param emailRecrutement  Recruitment contact shown in the offer
     */
    public record CompanyProfile(
            String nom,
            String secteur,
            String ville,
            int anneeFondation,
            String emailRecrutement
    ) {}
}
