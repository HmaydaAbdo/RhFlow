package com.hrflow.ingestion.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiting du endpoint {@code POST /ingest/cv} via token bucket Bucket4j.
 *
 * <p>Objectif : empêcher un flood (attaque ou bug n8n) de saturer la pipeline IA
 * et faire exploser les coûts LLM. Au-delà du quota → 429 Too Many Requests.
 *
 * <p>Algorithmie : Bucket4j gère un réservoir (capacity) qui se refill à un rythme
 * fixe (refillPerMinute). Chaque requête consomme 1 jeton. Si vide → rejet.
 *
 * <p>Tourne <strong>avant</strong> {@link IngestApiKeyFilter} (ordre câblé dans
 * {@code SecurityConfig}) : un attaquant qui flood SANS la clé est rejeté en ~1µs
 * sans même qu'on tente la comparaison constant-time de la clé. Économie de CPU.
 *
 * <p>Granularité actuelle : globale sur le path. Tous les clients partagent le
 * même bucket. Si demain on a plusieurs clés (multi-tenant), on pourra évoluer
 * vers un bucket par clé/IP via {@code ConcurrentHashMap<String, Bucket>}.
 *
 * <p>Backend : in-memory. Suffisant pour un backend single-instance.
 * Pour multi-instance (load balancing), basculer sur bucket4j-redis ou jcache.
 *
 * <p>Réponse au client :
 * <ul>
 *   <li>Succès → header {@code X-Rate-Limit-Remaining} pour informer le client.</li>
 *   <li>Rejet → header {@code Retry-After} (secondes) — n8n peut le lire et
 *       attendre avant de retry, comportement RFC 7231 standard.</li>
 * </ul>
 */
@Component
public class IngestRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IngestRateLimitFilter.class);

    private final Bucket bucket;

    public IngestRateLimitFilter(IngestProperties properties) {
        IngestProperties.RateLimit rl = properties.rateLimit();
        // Bandwidth : 1 limite = (capacity, refill rate).
        // On peut empiler plusieurs Bandwidth pour combiner contraintes (ex. 100/min ET 1000/h).
        Bandwidth limit = Bandwidth.builder()
                .capacity(rl.capacity())
                .refillGreedy(rl.refillPerMinute(), Duration.ofMinutes(1))
                .build();
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
        log.info("[Ingest] rate limit configuré : burst={}, refill={}/min",
                rl.capacity(), rl.refillPerMinute());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Même optimisation que IngestApiKeyFilter : on n'agit que sur /ingest/cv.
        return !IngestApiKeyFilter.INGEST_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // tryConsumeAndReturnRemaining : tente de consommer 1 jeton.
        // Retourne un probe qui indique si la consommation a réussi ET combien il
        // reste, OU dans combien de temps un nouveau jeton sera dispo.
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        // Bucket vide — calcul du délai avant le prochain jeton dispo (en secondes).
        long waitSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);

        log.warn("[Ingest] rate limit dépassé — Retry-After {}s", waitSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.addHeader("Retry-After", String.valueOf(waitSeconds));
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\"," +
                "\"message\":\"Trop de requêtes — réessayer dans %d s.\"}"
                        .formatted(waitSeconds));
    }
}
