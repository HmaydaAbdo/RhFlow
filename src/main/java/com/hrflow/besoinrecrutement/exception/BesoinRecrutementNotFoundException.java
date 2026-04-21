package com.hrflow.besoinrecrutement.exception;

public class BesoinRecrutementNotFoundException extends RuntimeException {

    public BesoinRecrutementNotFoundException(Long id) {
        super("Besoin en recrutement introuvable : id=" + id);
    }
}
