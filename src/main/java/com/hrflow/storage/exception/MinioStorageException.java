package com.hrflow.storage.exception;

/**
 * Exception levée pour toute défaillance de l'infrastructure MinIO :
 * upload, download, presign, ou suppression.
 *
 * Unchecked (extends RuntimeException) — les appelants n'ont pas à la
 * déclarer dans leur signature, mais peuvent la catcher pour renvoyer
 * un message d'erreur HTTP approprié.
 *
 * La cause originale (MinioException, IOException…) est toujours
 * préservée dans le champ `cause` pour le logging et le debugging.
 */
public class MinioStorageException extends RuntimeException {

    public MinioStorageException(String message) {
        super(message);
    }

    public MinioStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
