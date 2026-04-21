package com.hrflow.besoinrecrutement.exception;

public class BesoinRecrutementConflictException extends RuntimeException {

    public BesoinRecrutementConflictException(Long ficheDePosteId) {
        super("Un besoin en recrutement EN_COURS existe déjà pour la fiche de poste id=" + ficheDePosteId);
    }
}
