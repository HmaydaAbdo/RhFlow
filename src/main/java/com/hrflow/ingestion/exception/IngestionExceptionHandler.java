package com.hrflow.ingestion.exception;

import com.hrflow.shared.dtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Catcheur global des exceptions levées par la couche ingestion.
 *
 * <p>Convention :
 * <ul>
 *   <li>Le <b>body</b> renvoyé au client contient un message <b>destiné à
 *       l'utilisateur final</b> (français, sans jargon technique).</li>
 *   <li>Les ids techniques (record, candidature) sont écrits dans les
 *       <b>logs serveur</b>, pas dans la réponse — sauf {@code candidatureId}
 *       qui est utile au frontend pour proposer un lien direct.</li>
 *   <li>Chaque exception métier a son handler dédié, pas de fourre-tout.</li>
 * </ul>
 */
@RestControllerAdvice
public class IngestionExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(IngestionExceptionHandler.class);

    @ExceptionHandler(IngestionRecordNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(IngestionRecordNotFoundException ex) {
        log.info("[Ingestion] Record id={} introuvable", ex.getRecordId());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(IngestionAlreadyImportedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyImported(IngestionAlreadyImportedException ex) {
        log.info("[Ingestion] Record id={} déjà importé (candidature id={})",
                ex.getRecordId(), ex.getCandidatureId());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }
}
