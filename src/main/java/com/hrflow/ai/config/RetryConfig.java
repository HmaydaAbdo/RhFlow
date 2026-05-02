package com.hrflow.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Active le support Spring Retry via AOP (@Retryable, @Recover).
 *
 * Périmètre :
 *  - {@code DoclingService.toMarkdown()} — retry sur les erreurs HTTP transitoires
 *  - {@code AiGateway.extraire()} et {@code AiGateway.evaluer()} — retry sur les
 *    quotas/timeouts des providers LLM
 *
 * Paramètres par défaut (définis sur chaque @Retryable) :
 *  maxAttempts = 3, backoff exponentiel × 2 (1 s → 2 s).
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Activation uniquement — la configuration fine est sur chaque @Retryable.
}
