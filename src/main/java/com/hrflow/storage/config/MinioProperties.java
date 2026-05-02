package com.hrflow.storage.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriétés MinIO lues depuis application.yaml (préfixe "app.minio").
 *
 * @Validated → Spring valide les contraintes au démarrage.
 * Si une variable d'environnement est manquante ou vide, l'application
 * refuse de démarrer avec un message clair — pas de NullPointerException
 * silencieux au premier upload.
 *
 * Enregistrée via @EnableConfigurationProperties(MinioProperties.class)
 * dans MinioConfig — pas besoin de @Component.
 */
@Validated
@ConfigurationProperties(prefix = "app.minio")
public class MinioProperties {

    /** URL d'accès au serveur MinIO, ex: http://localhost:9000 */
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

    public String getEndpoint()   { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getAccessKey()  { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey()  { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
}
