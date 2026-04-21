package com.hrflow.direction.exception;

public class DirectionNotFoundException extends RuntimeException {
    public DirectionNotFoundException(Long id) {
        super("Direction introuvable avec l'identifiant : " + id);
    }
}
