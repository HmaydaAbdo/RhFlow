package com.hrflow.storage.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriétés MinIO lues depuis application.yaml (préfixe "app.minio").
 *
 * <h3>Deux endpoints, deux usages</h3>
 * <ul>
 *   <li>{@code endpoint} — endpoint interne, utilisé par le SDK pour les opérations
 *       S3 (upload, delete) et pour signer les presigned URLs consommées par
 *       docling-serve (Docker). Ex dev : {@code http://host.docker.internal:9000}.</li>
 *   <li>{@code publicEndpoint} — endpoint joignable depuis le navigateur, utilisé
 *       uniquement pour signer les presigned URLs de téléchargement browser.
 *       Ex dev : {@code http://localhost:9000}. Si vide, on replie sur {@code endpoint}.</li>
 * </ul>
 *
 * <p>SigV4 signe le header Host : chaque client signe avec son propre endpoint,
 * donc chaque URL reste valide sur son consommateur cible (docling OU browser).
 * On ne réécrit jamais une URL après signature.
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
     * URL canonique interne de MinIO — utilisée pour les opérations SDK (upload,
     * delete) et pour signer les presigned URLs consommées par docling-serve.
     * Ex dev local : {@code http://host.docker.internal:9000}.
     * Ex prod      : {@code http://minio:9000}.
     */
    @NotBlank(message = "app.minio.endpoint est requis")
    private String endpoint;

    /**
     * URL publique de MinIO joignable depuis le navigateur — utilisée uniquement
     * pour signer les presigned URLs de téléchargement browser.
     * Ex dev local : {@code http://localhost:9000}.
     * Ex prod      : {@code https://minio.mon-domaine.com}.
     * Si vide ou null, on utilise {@code endpoint} en repli.
     */
    private String publicEndpoint;

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
     * Optionnel — DNS override pour les sockets TCP sortants du SDK MinIO interne
     * uniquement. Format : {@code hostname=ip} (ex : {@code host.docker.internal=127.0.0.1}).
     * Vide ou null = pas d'override (résolution DNS standard).
     */
    private String sdkHostOverride;

    /** Renvoie l'endpoint public si défini, sinon l'endpoint interne. */
    public String effectivePublicEndpoint() {
        return (publicEndpoint != null && !publicEndpoint.isBlank()) ? publicEndpoint : endpoint;
    }

    public String getEndpoint()       { return endpoint; }
    public void   setEndpoint(String v) { this.endpoint = v; }

    public String getPublicEndpoint()       { return publicEndpoint; }
    public void   setPublicEndpoint(String v) { this.publicEndpoint = v; }

    public String getAccessKey()  { return accessKey; }
    public void   setAccessKey(String v) { this.accessKey = v; }

    public String getSecretKey()  { return secretKey; }
    public void   setSecretKey(String v) { this.secretKey = v; }

    public String getBucketName() { return bucketName; }
    public void   setBucketName(String v) { this.bucketName = v; }

    public String getSdkHostOverride() { return sdkHostOverride; }
    public void   setSdkHostOverride(String v) { this.sdkHostOverride = v; }
}
