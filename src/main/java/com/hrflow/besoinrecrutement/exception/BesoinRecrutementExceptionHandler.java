package com.hrflow.besoinrecrutement.exception;

import com.hrflow.shared.dtos.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BesoinRecrutementExceptionHandler {

    @ExceptionHandler(BesoinRecrutementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(BesoinRecrutementNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(BesoinRecrutementConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(BesoinRecrutementConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(BesoinRecrutementAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(BesoinRecrutementAccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden", ex.getMessage()));
    }
}
