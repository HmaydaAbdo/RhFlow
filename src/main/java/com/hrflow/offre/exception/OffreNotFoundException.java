package com.hrflow.offre.exception;

public class OffreNotFoundException extends RuntimeException {

    public OffreNotFoundException(Long projetId) {
        super("Aucune offre générée pour le projet de recrutement id=" + projetId);
    }
}
