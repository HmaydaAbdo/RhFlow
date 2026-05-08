package com.hrflow.storage.service;

import com.hrflow.storage.config.MinioProperties;
import com.hrflow.storage.exception.MinioStorageException;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Façade sur le client MinIO SDK.
 *
 * <p>Deux méthodes de presign distinctes selon le consommateur :
 * <ul>
 *   <li>{@link #presignedUrl} — signe avec l'endpoint interne ({@code host.docker.internal}).
 *       Destinée à docling-serve (Docker résout ce hostname).</li>
 *   <li>{@link #presignedUrlForBrowser} — signe avec l'endpoint public ({@code localhost:9000}).
 *       Retourne une URL téléchargeable directement par le navigateur, avec
 *       {@code Content-Disposition: attachment} pour forcer le download.</li>
 * </ul>
 *
 * <p>On ne réécrit jamais une URL après signature (SigV4 signe le header Host).
 */
@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private static final int    PRESIGN_EXPIRY_MINUTES = 15;
    private static final String FALLBACK_CONTENT_TYPE  = "application/octet-stream";
    private static final long   MULTIPART_PART_SIZE    = 10 * 1024 * 1024L;

    /** Client interne : endpoint canonique + DNS override. */
    private final MinioClient     client;
    /** Client public : endpoint joignable depuis le navigateur, sans DNS override. */
    private final MinioClient     publicClient;
    private final MinioProperties props;

    public MinioService(MinioClient client,
                        @Qualifier("public") MinioClient publicClient,
                        MinioProperties props) {
        this.client       = client;
        this.publicClient = publicClient;
        this.props        = props;
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    @PostConstruct
    void init() {
        try {
            boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(props.getBucketName()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.getBucketName()).build());
                log.info("[MinIO] bucket '{}' créé", props.getBucketName());
            } else {
                log.info("[MinIO] bucket '{}' trouvé", props.getBucketName());
            }
        } catch (Exception e) {
            log.warn("[MinIO] impossible de vérifier le bucket au démarrage : {} — " +
                     "vérifiez que MinIO est accessible.", e.getMessage());
        }
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    public void upload(String objectPath, MultipartFile file) {
        long   fileSize    = file.getSize();
        long   size        = fileSize > 0 ? fileSize : -1L;
        long   partSize    = fileSize > 0 ? -1L       : MULTIPART_PART_SIZE;
        String contentType = resolveContentType(file);

        try (InputStream is = file.getInputStream()) {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(props.getBucketName())
                    .object(objectPath)
                    .stream(is, size, partSize)
                    .contentType(contentType)
                    .build());
            log.info("[MinIO] uploadé '{}' ({} bytes, type={})", objectPath, fileSize, contentType);
        } catch (Exception e) {
            log.error("[MinIO] upload échoué pour '{}' : {}", objectPath, e.getMessage());
            throw new MinioStorageException("Impossible de stocker le fichier dans MinIO", e);
        }
    }

    // ── Presigned URL — usage interne (docling) ───────────────────────────────

    /**
     * Génère une URL GET présignée via le client interne ({@code host.docker.internal}).
     * Consommée par docling-serve qui tourne dans Docker et résout ce hostname.
     */
    public String presignedUrl(String objectPath) {
        try {
            return client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.getBucketName())
                    .object(objectPath)
                    .expiry(PRESIGN_EXPIRY_MINUTES, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("[MinIO] presign (interne) échoué pour '{}' : {}", objectPath, e.getMessage());
            throw new MinioStorageException("Impossible de générer le lien de téléchargement", e);
        }
    }

    // ── Presigned URL — usage navigateur ─────────────────────────────────────

    /**
     * Génère une URL GET présignée via le client public ({@code localhost:9000}).
     * Destinée à être ouverte directement par le navigateur.
     *
     * <p>Ajoute le query-param {@code response-content-disposition=attachment;filename=...}
     * afin que MinIO retourne un header {@code Content-Disposition: attachment},
     * ce qui déclenche le téléchargement immédiat au lieu d'un affichage inline.
     *
     * @param objectPath chemin de l'objet dans le bucket
     * @param fileName   nom de fichier suggéré dans la boîte de dialogue de téléchargement
     */
    public String presignedUrlForBrowser(String objectPath, String fileName) {
        String disposition = "attachment; filename=\"" + sanitizeFileName(fileName) + "\"";
        String encodedDisposition = URLEncoder.encode(disposition, StandardCharsets.UTF_8);

        try {
            return publicClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.getBucketName())
                    .object(objectPath)
                    .expiry(PRESIGN_EXPIRY_MINUTES, TimeUnit.MINUTES)
                    .extraQueryParams(Map.of("response-content-disposition", encodedDisposition))
                    .build());
        } catch (Exception e) {
            log.error("[MinIO] presign (browser) échoué pour '{}' : {}", objectPath, e.getMessage());
            throw new MinioStorageException("Impossible de générer le lien de téléchargement", e);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(String objectPath) {
        try {
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(props.getBucketName())
                    .object(objectPath)
                    .build());
            log.info("[MinIO] supprimé '{}'", objectPath);
        } catch (Exception e) {
            log.warn("[MinIO] suppression échouée pour '{}' : {} — " +
                     "l'objet MinIO peut nécessiter une suppression manuelle.", objectPath, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        return (ct != null && !ct.isBlank()) ? ct : FALLBACK_CONTENT_TYPE;
    }

    /** Retire les guillemets du nom de fichier pour éviter de casser l'header. */
    private static String sanitizeFileName(String name) {
        return (name == null) ? "cv" : name.replace("\"", "").replace("\\", "");
    }
}
