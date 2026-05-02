package com.hrflow.candidature.exception;

public class CandidatureNotFoundException extends RuntimeException {
    public CandidatureNotFoundException(Long id) {
        super("Candidature introuvable avec l'id=" + id);
    }
}
