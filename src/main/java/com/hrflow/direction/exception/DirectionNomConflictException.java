// src/main/java/com/hrflow/recruitment/direction/exception/DirectionNomConflictException.java
package com.hrflow.direction.exception;

public class DirectionNomConflictException extends RuntimeException {
    public DirectionNomConflictException(String nom) {
        super("Une direction avec le nom '" + nom + "' existe déjà");
    }
}
