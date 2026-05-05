package com.hrflow.storage.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriétés MinIO lues depuis application.yaml (préfixe "app.minio").
 *
 * <h3>Pourquoi un seul endpoint mais un DNS override ?</h3>
 * Spring Boot tourne sur l'hôte alors que docling-serve tourne dans Docker.
 * Pour que les presigned URLs (signées par AWS Signature V4) restent valides
 * côté docling, le hostname doit être identique entre :
 *   - l'endpoint utilisé par le SDK pour signer (canonical request inclut Host)
 *   - le hostname inscrit dans l'URL retournée
 *
 * <p>En dev local, on utilise donc {@code http://host.docker.internal:9000}
 * partout. Mais le process JVM hôte ne peut pas toujours résoudre ce hostname
 * vers une IP joignable (Docker Desktop sur Windows met parfois une IP LAN
 * non-routable). On contourne via {@link #sdkHostOverride} : un mapping
 * {@code hostname=ip} appliqué UNIQUEMENT à la couche TCP du SDK MinIO
 * (custom OkHttp DNS), sans toucher au hostname signé.
 *
 * <p>{@code @Validated} : Spring valide les contraintes au démarrage.
 *
 * <p>Enregistrée via {@code @EnableConfigurationProperties(MinioProperties.class)}
 * dans {@link MinioConfig}.
 */
@Validated
@ConfigurationProperties(prefix = "app.minio")
public class MinioProperties {

    /**
     * URL canonique de MinIO, utilisée à la fois pour signer les presigned URLs
     * et comme hostname dans les URLs retournées.
     * Ex dev local : {@code http://host.docker.internal:9000}.
     * Ex prod : {@code http://minio:9000}.
     */
    @NotBlank(message = "app.minio.endpoint est requis")
    private String endpoint;

    /** Clé d'accès (équivalent username). */
    @NotBlank(message = "app.minio.access-key est requis")
    private String accessKey;

    /** Clé secrète (équivalent password). */
    @NotBlank(message = "app.minio.secret-key est requis")
    private String secretKey;

    /** Nom du bucket où les CVs sont stockés. */
    @NotBlank(message = "app.minio.bucket-name est requis")
    private String bucketName;

    /**
     * Optionnel — DNS override pour les sockets TCP sortants du SDK MinIO uniquement.
     * Format : {@code hostname=ip} (ex : {@code host.docker.internal=127.0.0.1}).
     * Vide ou null = pas d'override (résolution DNS standard).
     *
     * <p>Cas d'usage : Spring Boot tourne sur l'hôte, MinIO est exposé via
     * docker-compose sur {@code localhost:9000}, mais l'endpoint canonique
     * (utilisé pour signer + dans les URLs) doit être {@code host.docker.internal}
     * pour que docling-serve puisse les résoudre. Le SDK signe correctement
     * et la TCP est routée vers localhost, sans toucher la signature.
     */
    private String sdkHostOverride;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getAccessKey()  { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey()  { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }

    public String getSdkHostOverride() { return sdkHostOverride; }
    public void setSdkHostOverride(String sdkHostOverride) { this.sdkHostOverride = sdkHostOverride; }
}
