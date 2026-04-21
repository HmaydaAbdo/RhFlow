package com.hrflow.besoinrecrutement.exception;

public class BesoinRecrutementAccessDeniedException extends RuntimeException {

    public BesoinRecrutementAccessDeniedException() {
        super("Accès refusé : vous n'avez pas les droits sur ce besoin en recrutement");
    }

    public BesoinRecrutementAccessDeniedException(String message) {
        super(message);
    }
}
