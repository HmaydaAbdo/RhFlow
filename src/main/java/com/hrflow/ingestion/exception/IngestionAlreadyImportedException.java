package com.hrflow.ingestion.exception;

/**
 * Levée quand le DRH tente une action de rattachement ou de rejet manuel sur
 * un record déjà importé en tant que candidature. Un record importé est
 * terminal — il a produit une candidature, on ne le re-traite pas.
 *
 * <p>Mappée vers <strong>HTTP 409 Conflict</strong> par
 * {@link IngestionExceptionHandler}. Le message est destiné à l'utilisateur
 * final (DRH). L'id de la candidature liée est exposé via {@link #getCandidatureId()}
 * pour permettre au frontend de proposer un lien direct (« Voir la candidature »).
 */
public class IngestionAlreadyImportedException extends RuntimeException {

    private final Long recordId;
    private final Long candidatureId;

    public IngestionAlreadyImportedException(Long recordId, Long candidatureId) {
        super("Ce CV a déjà été importé en tant que candidature.");
        this.recordId      = recordId;
        this.candidatureId = candidatureId;
    }

    public Long getRecordId()      { return recordId; }
    public Long getCandidatureId() { return candidatureId; }
}
