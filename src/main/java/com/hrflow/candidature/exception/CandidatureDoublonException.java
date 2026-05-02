package com.hrflow.candidature.exception;

public class CandidatureDoublonException extends RuntimeException {
    public CandidatureDoublonException(String email, Long projetId) {
        super("Le candidat '" + email + "' a déjà postulé au projet id=" + projetId);
    }
}
