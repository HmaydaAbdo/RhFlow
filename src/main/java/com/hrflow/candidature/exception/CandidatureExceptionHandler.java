package com.hrflow.candidature.exception;

import com.hrflow.shared.dtos.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CandidatureExceptionHandler {

    @ExceptionHandler(CandidatureNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(CandidatureNotFoundException ex) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage());
    }

    @ExceptionHandler(CandidatureDoublonException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDoublon(CandidatureDoublonException ex) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage());
    }

    /**
     * Couvre :
     *  - statut invalide dans changerStatut() ("Seuls RETENU et REJETE…")
     *  - re-évaluation déjà en cours dans reevaluer()
     *  - dépassement de pages dans validatePageCount()
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage());
    }
}
