// src/main/java/com/hrflow/recruitment/direction/exception/DirectionExceptionHandler.java
package com.hrflow.direction.exception;

import com.hrflow.shared.dtos.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.hrflow.recruitment.direction")
public class DirectionExceptionHandler {

    @ExceptionHandler(DirectionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DirectionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(DirectionNomConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DirectionNomConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(DirecteurRoleException.class)
    public ResponseEntity<ErrorResponse> handleDirecteurRole(DirecteurRoleException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(422, "Unprocessable Entity", ex.getMessage()));
    }
}
