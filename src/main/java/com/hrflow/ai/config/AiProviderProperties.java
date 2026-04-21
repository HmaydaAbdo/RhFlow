package com.hrflow.ai.config;

/**
 * Configuration record for a single AI provider (Groq, Google AI Studio,
 * Cerebras, Mistral, Ollama, …). All providers use the OpenAI-compatible API,
 * so the same record shape works for every one of them.
 */
public record AiProviderProperties(

        /** Human-readable label used in logs (e.g. "groq", "google", "ollama"). */
        String name,

        /** OpenAI-compatible base URL (no trailing slash needed). */
        String baseUrl,

        /** API key; use "ollama" for local Ollama which ignores it. */
        String apiKey,

        /** Model name as expected by the provider (e.g. "llama-3.3-70b-versatile"). */
        String model,

        /**
         * Request timeout in seconds. Keep low for cloud providers so the fallback
         * kicks in quickly on a hung connection. Longer for Ollama (local is slow).
         */
        int timeoutSeconds,

        /** Set to false to skip a provider without removing it from config. */
        boolean enabled,

        /**
         * Sampling temperature [0.0 – 2.0].
         * 0.0 = deterministic (good for structured extraction / CV analysis).
         * 0.75 = balanced creativity (good for job offer generation).
         * Defaults to 0.75 if not set in YAML.
         */
        double temperature
) {
    /** Compact constructor — applies sensible defaults for optional fields. */
    public AiProviderProperties {
        if (timeoutSeconds <= 0) timeoutSeconds = 30;
        if (name      == null)   name        = "unnamed";
        if (baseUrl   == null)   throw new IllegalArgumentException("AI provider 'baseUrl' is required");
        if (apiKey    == null)   apiKey      = "none";
        if (model     == null)   throw new IllegalArgumentException("AI provider 'model' is required");
        if (temperature <= 0.0)  temperature = 0.75;
    }
}