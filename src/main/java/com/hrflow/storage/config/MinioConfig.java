package com.hrflow.storage.config;

import io.minio.MinioClient;
import io.minio.http.HttpUtils;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Configuration MinIO.
 *
 * <p>{@code @EnableConfigurationProperties} enregistre {@link MinioProperties}
 * comme bean Spring et déclenche la validation {@code @NotBlank} au démarrage —
 * sans avoir besoin de {@code @Component} sur {@code MinioProperties} elle-même.
 *
 * <h3>OkHttpClient avec DNS override</h3>
 * Le SDK MinIO utilise OkHttp en interne. On lui injecte un client custom dont
 * le résolveur DNS peut surcharger UN hostname précis vers une IP fixe. Cela
 * permet de garder un endpoint canonique (ex : {@code host.docker.internal})
 * pour la signature des presigned URLs, tout en routant les sockets TCP du SDK
 * vers une IP joignable depuis le process JVM (ex : {@code 127.0.0.1}).
 *
 * <p>Si {@code app.minio.sdk-host-override} est vide, on utilise le client
 * OkHttp par défaut du SDK MinIO — comportement standard, aucun override.
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    public MinioClient minioClient(MinioProperties props) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey());

        OkHttpClient httpClient = buildHttpClient(props);
        if (httpClient != null) {
            builder.httpClient(httpClient);
        }
        return builder.build();
    }

    /**
     * Construit un OkHttpClient avec DNS override si configuré.
     * Renvoie {@code null} si aucun override → on laisse le SDK MinIO utiliser
     * son client par défaut (timeouts/connection-pool tunés).
     */
    private OkHttpClient buildHttpClient(MinioProperties props) {
        HostOverride override = parseOverride(props.getSdkHostOverride());
        if (override == null) {
            return null;
        }

        log.info("[MinIO] DNS override actif pour le SDK : {} → {} (TCP only, signature non affectée)",
                 override.hostname(), override.ip());

        // Repart de la base OkHttp préconfigurée par MinIO (timeouts, retry, etc.)
        // pour ne pas perdre le tuning du SDK, et y ajoute notre Dns custom.
        OkHttpClient base = HttpUtils.newDefaultHttpClient(
                /* connectTimeout */ 10_000,
                /* writeTimeout   */ 60_000,
                /* readTimeout    */ 60_000);

        return base.newBuilder()
                .dns(new HostOverrideDns(override))
                .build();
    }

    /**
     * Parse un mapping {@code "hostname=ip"}. Renvoie null si vide/invalide
     * (avec un warn pour ne pas masquer une mauvaise config silencieusement).
     */
    private HostOverride parseOverride(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int eq = raw.indexOf('=');
        if (eq <= 0 || eq == raw.length() - 1) {
            log.warn("[MinIO] sdk-host-override mal formé : '{}' (attendu hostname=ip) — ignoré", raw);
            return null;
        }
        String hostname = raw.substring(0, eq).trim();
        String ip       = raw.substring(eq + 1).trim();
        if (hostname.isEmpty() || ip.isEmpty()) {
            log.warn("[MinIO] sdk-host-override vide après split : '{}' — ignoré", raw);
            return null;
        }
        return new HostOverride(hostname, ip);
    }

    /** Mapping immutable hostname → IP. */
    private record HostOverride(String hostname, String ip) {}

    /**
     * Résolveur DNS qui remplace UN hostname précis par UNE IP fixe ;
     * tous les autres hostnames passent par la résolution système standard.
     */
    private static final class HostOverrideDns implements Dns {

        private final String hostname;
        private final InetAddress overriddenIp;

        HostOverrideDns(HostOverride override) {
            this.hostname = override.hostname();
            try {
                // getByName d'une IP littérale ne fait pas de résolution réseau.
                this.overriddenIp = InetAddress.getByName(override.ip());
            } catch (UnknownHostException e) {
                throw new IllegalStateException(
                        "IP invalide pour app.minio.sdk-host-override : " + override.ip(), e);
            }
        }

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            if (this.hostname.equalsIgnoreCase(hostname)) {
                return List.of(overriddenIp);
            }
            return Dns.SYSTEM.lookup(hostname);
        }
    }
}
