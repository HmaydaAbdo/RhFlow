package com.hrflow.projetrecrutement.exception;

import com.hrflow.shared.dtos.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProjetRecrutementExceptionHandler {

    @ExceptionHandler(ProjetRecrutementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProjetRecrutementNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjetRecrutementAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(ProjetRecrutementAccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(ProjetRecrutementAlreadyClosedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyClosed(ProjetRecrutementAlreadyClosedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjetRecrutementConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ProjetRecrutementConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }
}
