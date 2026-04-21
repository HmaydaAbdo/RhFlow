package com.hrflow.projetrecrutement.exception;

public class ProjetRecrutementNotFoundException extends RuntimeException {
    public ProjetRecrutementNotFoundException(Long id) {
        super("Projet de recrutement introuvable : id=" + id);
    }
}
