package com.hrflow.projetrecrutement.exception;

public class ProjetRecrutementAlreadyClosedException extends RuntimeException {
    public ProjetRecrutementAlreadyClosedException(Long id) {
        super("Le projet de recrutement id=" + id + " est déjà fermé");
    }
}
