package com.hrflow.ai.exception;

import com.hrflow.besoinrecrutement.model.StatutBesoin;

public class OfferGenerationNotAllowedException extends RuntimeException {

    public OfferGenerationNotAllowedException(Long besoinId, StatutBesoin statut) {
        super("La génération d'offre n'est autorisée que pour les besoins acceptés. " +
              "Le besoin id=" + besoinId + " est actuellement en statut : " + statut.name());
    }
}
