package com.hrflow.fichedeposte.exception;

public class FicheDePosteNotFoundException extends RuntimeException {
    public FicheDePosteNotFoundException(Long id) {
        super("Fiche de poste introuvable avec l'identifiant : " + id);
    }
}
