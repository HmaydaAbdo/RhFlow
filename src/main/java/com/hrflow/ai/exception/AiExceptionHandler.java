package com.hrflow.ai.exception;

import com.hrflow.shared.dtos.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(OfferGenerationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleOfferNotAllowed(OfferGenerationNotAllowedException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage()));
    }

    /**
     * Fires when every provider in the fallback chain has been exhausted
     * (all hit their rate limit or timed out simultaneously).
     * Returns 503 so the frontend can display a user-friendly "AI unavailable" message.
     */
    @ExceptionHandler(AllProvidersExhaustedException.class)
    public ResponseEntity<ErrorResponse> handleAllProvidersExhausted(AllProvidersExhaustedException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(503, "Service Unavailable",
                        "Le service d'intelligence artificielle est temporairement indisponible. Veuillez réessayer dans quelques instants."));
    }
}
