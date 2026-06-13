package com.hrflow.ingestion.exception;

/**
 * Levée quand un {@code IngestionRecord} référencé par id n'existe pas.
 *
 * <p>Cas d'usage typiques côté UI DRH :
 * <ul>
 *   <li>L'utilisateur ouvre une ligne « CV reçu » qui a été supprimée par
 *       quelqu'un d'autre entre-temps.</li>
 *   <li>Action manuelle (rattachement / rejet) sur un id obsolète (page
 *       laissée ouverte longtemps).</li>
 * </ul>
 *
 * <p>Mappée vers <strong>HTTP 404 Not Found</strong> par
 * {@link IngestionExceptionHandler}. Le message est destiné à l'utilisateur
 * final (DRH) ; l'id technique est conservé en champ pour le log applicatif.
 */
public class IngestionRecordNotFoundException extends RuntimeException {

    private final Long recordId;

    public IngestionRecordNotFoundException(Long recordId) {
        super("Ce CV reçu est introuvable. Il a peut-être déjà été supprimé.");
        this.recordId = recordId;
    }

    /** Id technique du record introuvable — pour les logs, pas pour l'utilisateur. */
    public Long getRecordId() {
        return recordId;
    }
}
