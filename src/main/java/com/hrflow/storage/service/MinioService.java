package com.hrflow.storage.service;

import com.hrflow.storage.config.MinioProperties;
import com.hrflow.storage.exception.MinioStorageException;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Façade sur le client MinIO SDK.
 *
 * Toutes les erreurs sont remontées sous forme de {@link MinioStorageException}
 * (unchecked) afin que les appelants puissent les distinguer d'autres RuntimeExceptions
 * et les transformer en réponses HTTP adaptées.
 *
 * Contrats importants :
 *  - upload()       : lance MinioStorageException si MinIO est inaccessible ou refuse le fichier.
 *  - presignedUrl() : retourne une URL GET valide {@value PRESIGN_EXPIRY_MINUTES} minutes.
 *  - delete()    : fail-soft — log warn sans throw pour ne pas faire échouer
 *                  la transaction DB si MinIO est temporairement indisponible.
 */
@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    /** Durée de validité des URLs présignées (GET). Suffisant pour un téléchargement direct. */
    private static final int PRESIGN_EXPIRY_MINUTES = 15;

    /** Content-type de repli quand Spring ne peut pas déterminer le type du fichier uploadé. */
    private static final String FALLBACK_CONTENT_TYPE = "application/octet-stream";

    /**
     * Taille de part utilisée en multipart upload quand la taille du fichier est inconnue.
     * MinIO SDK exige une valeur > 0 dans ce cas (5 MB minimum imposé par S3).
     */
    private static final long MULTIPART_PART_SIZE = 10 * 1024 * 1024L; // 10 MB

    private final MinioClient     client;
    private final MinioProperties props;

    public MinioService(MinioClient client, MinioProperties props) {
        this.client = client;
        this.props  = props;
    }

    // ── Initialisation ───────────────────────────────────────────────────────────

    /**
     * Vérifie / crée le bucket après l'initialisation du contexte Spring.
     * Fail-open : si MinIO est indisponible au boot, un warning est loggé et
     * l'application démarre quand même — l'erreur se révèlera au premier upload.
     */
    @PostConstruct
    void init() {
        try {
            boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(props.getBucketName()).build()
            );
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

    // ── Upload ───────────────────────────────────────────────────────────────────

    /**
     * Uploade un fichier vers MinIO en streaming.
     *
     * Gestion de la taille :
     *  - Si {@code file.getSize()} est connu (> 0), on le passe directement au SDK
     *    et partSize est ignoré (-1).
     *  - Si la taille est inconnue (retourne -1 ou 0), on active le multipart
     *    avec {@value MULTIPART_PART_SIZE} bytes par part.
     *
     * @param objectPath chemin complet de l'objet dans le bucket (ex: "projets/1/cvs/uuid.pdf")
     * @param file       fichier uploadé via HTTP multipart
     * @throws MinioStorageException si l'upload échoue
     */
    public void upload(String objectPath, MultipartFile file) {
        long fileSize  = file.getSize();
        long size      = fileSize > 0 ? fileSize  : -1L;
        long partSize  = fileSize > 0 ? -1L       : MULTIPART_PART_SIZE;
        String contentType = resolveContentType(file);

        try (InputStream is = file.getInputStream()) {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(props.getBucketName())
                    .object(objectPath)
                    .stream(is, size, partSize)
                    .contentType(contentType)
                    .build()
            );
            log.info("[MinIO] uploadé '{}' ({} bytes, type={})", objectPath, fileSize, contentType);
        } catch (Exception e) {
            log.error("[MinIO] upload échoué pour '{}' : {}", objectPath, e.getMessage());
            throw new MinioStorageException("Impossible de stocker le fichier dans MinIO", e);
        }
    }

    // ── Presigned URL ────────────────────────────────────────────────────────────

    /**
     * Génère une URL GET présignée valide {@value PRESIGN_EXPIRY_MINUTES} minutes.
     * Le frontend peut télécharger le CV directement depuis MinIO sans repasser
     * par le backend Java.
     *
     * @param objectPath chemin de l'objet dans le bucket
     * @return URL présignée
     * @throws MinioStorageException si la génération échoue
     */
    public String presignedUrl(String objectPath) {
        try {
            return client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.getBucketName())
                    .object(objectPath)
                    .expiry(PRESIGN_EXPIRY_MINUTES, TimeUnit.MINUTES)
                    .build()
            );
        } catch (Exception e) {
            log.error("[MinIO] presign échoué pour '{}' : {}", objectPath, e.getMessage());
            throw new MinioStorageException("Impossible de générer le lien de téléchargement", e);
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────────

    /**
     * Supprime un objet de MinIO.
     * <p>
     * Fail-soft intentionnel : si MinIO rate la suppression, un warning est loggé
     * mais aucune exception n'est levée. Cela permet à la transaction DB (suppression
     * de la candidature) de se terminer proprement même si MinIO est temporairement
     * indisponible. Une réconciliation manuelle ou un job de nettoyage peut être
     * mis en place ultérieurement si nécessaire.
     *
     * @param objectPath chemin de l'objet à supprimer
     */
    public void delete(String objectPath) {
        try {
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(props.getBucketName())
                    .object(objectPath)
                    .build()
            );
            log.info("[MinIO] supprimé '{}'", objectPath);
        } catch (Exception e) {
            log.warn("[MinIO] suppression échouée pour '{}' : {} — " +
                     "l'objet MinIO peut nécessiter une suppression manuelle.", objectPath, e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Résout le content-type du fichier uploadé.
     * Si Spring ne peut pas le déterminer (retourne null ou vide),
     * on retombe sur "application/octet-stream" plutôt que d'envoyer
     * "null" comme content-type à MinIO.
     */
    private static String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        return (ct != null && !ct.isBlank()) ? ct : FALLBACK_CONTENT_TYPE;
    }
}
