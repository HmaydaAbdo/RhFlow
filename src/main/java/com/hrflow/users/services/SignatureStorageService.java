package com.hrflow.users.services;

import com.hrflow.storage.service.MinioService;
import com.hrflow.users.entities.User;
import com.hrflow.users.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class SignatureStorageService {

    private static final Logger log = LoggerFactory.getLogger(SignatureStorageService.class);

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg");
    private static final long MAX_SIZE_BYTES = 500 * 1024L; // 500 KB

    private final MinioService minioService;
    private final UserRepository userRepository;

    public SignatureStorageService(MinioService minioService, UserRepository userRepository) {
        this.minioService   = minioService;
        this.userRepository = userRepository;
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    @Transactional
    public void store(Long userId, MultipartFile file) {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable : " + userId));

        String contentType = file.getContentType();
        String extension   = "image/png".equals(contentType) ? "png" : "jpg";
        String objectPath  = "signatures/user-" + userId + "." + extension;

        // Supprimer l'ancienne signature si elle existait sous un nom différent
        String existingKey = user.getSignatureKey();
        if (existingKey != null && !existingKey.equals(objectPath)) {
            minioService.delete(existingKey);
            log.info("[Signature] ancienne clé supprimée : {}", existingKey);
        }

        minioService.upload(objectPath, file);

        user.setSignatureKey(objectPath);
        user.setSignatureContentType(contentType);
        userRepository.save(user);

        log.info("[Signature] stockée pour userId={} → {}", userId, objectPath);
    }

    // ── Fetch (prévisualisation) ──────────────────────────────────────────────

    /**
     * Retourne les bytes et le content-type de la signature d'un utilisateur.
     * Lève 404 si l'utilisateur n'existe pas ou n'a pas de signature.
     */
    public SignatureData fetch(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable : " + userId));

        if (user.getSignatureKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Cet utilisateur n'a pas de signature enregistrée");
        }

        byte[] bytes = minioService.getBytes(user.getSignatureKey());
        String contentType = user.getSignatureContentType() != null
                ? user.getSignatureContentType() : "image/png";
        return new SignatureData(bytes, contentType);
    }

    /** DTO de retour pour la prévisualisation de signature. */
    public record SignatureData(byte[] bytes, String contentType) {}

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable : " + userId));

        String existingKey = user.getSignatureKey();
        if (existingKey == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Cet utilisateur n'a pas de signature enregistrée");
        }

        minioService.delete(existingKey);

        user.setSignatureKey(null);
        user.setSignatureContentType(null);
        userRepository.save(user);

        log.info("[Signature] supprimée pour userId={}", userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide ou absent");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Format non supporté. Seuls PNG et JPG sont acceptés.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Fichier trop volumineux. Taille maximale : 500 Ko.");
        }
    }
}
