package com.hrflow.ingestion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Filtre de sécurité dédié à l'endpoint technique {@code POST /ingest/cv}.
 *
 * <p>Pattern « API-key partagée » : n8n présente la clé dans le header
 * {@code X-Ingest-Key}, ce filtre la compare à {@code app.ingest.api-key}
 * (variable d'env {@code INGEST_API_KEY}). Si OK → installe une authentication
 * synthétique avec l'authority {@code INGEST}. Sinon → 401 immédiat.
 *
 * <p>Rationale : éviter la lourdeur d'un user de service + JWT + refresh pour
 * une intégration machine-to-machine. La clé tourne en changeant l'env.
 *
 * <p>Garanties de sécurité :
 * <ul>
 *   <li><b>Fail-secure</b> : si la propriété est absente ou vide, le filtre
 *       refuse tous les appels (pas de bypass silencieux).</li>
 *   <li><b>Constant-time comparison</b> : {@link MessageDigest#isEqual} évite
 *       les attaques par timing sur la clé.</li>
 *   <li><b>Path strict</b> : {@link #shouldNotFilter} court-circuite immédiatement
 *       sur toute URL ≠ {@code /ingest/cv} — aucun impact sur le reste de l'API.</li>
 * </ul>
 */
@Component
public class IngestApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IngestApiKeyFilter.class);

    public static final String INGEST_PATH      = "/ingest/cv";
    public static final String HEADER_NAME      = "X-Ingest-Key";
    public static final String INGEST_AUTHORITY = "INGEST";

    private final IngestProperties properties;

    public IngestApiKeyFilter(IngestProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // On ne joue le filtre QUE sur l'endpoint dédié — économie sur tout le reste de l'API.
        return !INGEST_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String expected = properties.apiKey();
        if (!StringUtils.hasText(expected)) {
            log.error("[Ingest] app.ingest.api-key non configurée — refus de tous les appels (fail-secure)");
            reject(response, "Ingest endpoint not configured");
            return;
        }

        String presented = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(presented)) {
            log.warn("[Ingest] header {} manquant — refus", HEADER_NAME);
            reject(response, "Missing " + HEADER_NAME);
            return;
        }

        if (!constantTimeEquals(expected, presented)) {
            log.warn("[Ingest] clé invalide — refus");
            reject(response, "Invalid " + HEADER_NAME);
            return;
        }

        // Authentification synthétique — l'endpoint utilisera @PreAuthorize("hasAuthority('INGEST')").
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(INGEST_AUTHORITY));
        AbstractAuthenticationToken auth = new AbstractAuthenticationToken(authorities) {
            @Override public Object getCredentials() { return "N/A"; }
            @Override public Object getPrincipal()   { return "n8n-ingest"; }
        };
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void reject(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}".formatted(reason));
    }

    /** Comparaison à temps constant pour empêcher les attaques par timing. */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
