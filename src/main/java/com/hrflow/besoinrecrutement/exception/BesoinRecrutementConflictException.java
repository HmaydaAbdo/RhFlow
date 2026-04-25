package com.hrflow.besoinrecrutement.exception;

public class BesoinRecrutementConflictException extends RuntimeException {

    public BesoinRecrutementConflictException(Long ficheDePosteId) {
        super("Un besoin en attente de décision existe déjà pour la fiche de poste id=" + ficheDePosteId);
    }

    public BesoinRecrutementConflictException(String message) {
        super(message);
    }
}
