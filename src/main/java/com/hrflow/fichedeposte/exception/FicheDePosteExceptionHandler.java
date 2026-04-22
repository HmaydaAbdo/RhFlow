// src/main/java/com/hrflow/recruitment/fichedeposte/exception/FicheDePosteExceptionHandler.java
package com.hrflow.fichedeposte.exception;

import com.hrflow.shared.dtos.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.hrflow.fichedeposte")
public class FicheDePosteExceptionHandler {

    @ExceptionHandler(FicheDePosteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(FicheDePosteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(FicheDePosteAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(FicheDePosteAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(FicheDePosteHasBesoinsException.class)
    public ResponseEntity<ErrorResponse> handleHasBesoins(FicheDePosteHasBesoinsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict",
                        "Impossible de supprimer cette fiche de poste : des besoins de recrutement y sont encore rattachés."));
    }
}
