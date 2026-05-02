package com.hrflow.docling.exception;

/**
 * Exception levée lorsque docling-serve est inaccessible ou que la conversion
 * d'un document (PDF, DOCX) vers Markdown a échoué.
 *
 * Unchecked — les appelants n'ont pas à la déclarer dans leur signature.
 * Traitée par {@code GlobalExceptionHandler} → 503 Service Unavailable.
 *
 * Causes possibles :
 *  - docling-serve est down ou non joignable depuis Spring Boot
 *  - Le document est corrompu ou dans un format non supporté
 *  - Timeout réseau dépassé (configurable via app.docling.timeout-seconds)
 */
public class DoclingConversionException extends RuntimeException {

    public DoclingConversionException(String message) {
        super(message);
    }

    public DoclingConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
