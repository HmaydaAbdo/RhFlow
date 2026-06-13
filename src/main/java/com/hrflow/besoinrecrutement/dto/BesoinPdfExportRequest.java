package com.hrflow.besoinrecrutement.dto;

/**
 * Sélection des champs à inclure dans l'export PDF d'un besoin de recrutement.
 * Le document est destiné à la Direction Générale pour validation (3 signatures).
 *
 * L'intitulé du poste est toujours inclus (titre + référence).
 * Chaque booléen contrôle l'inclusion du champ dans le texte narratif.
 */
public record BesoinPdfExportRequest(

        ContexteFields  contexte,
        FicheFields     fiche

) {

    /**
     * Champs de la section I — Présentation de la demande.
     * Puisés dans le besoin de recrutement.
     */
    public record ContexteFields(
            boolean direction,
            boolean directeur,
            boolean lieuAffectation,
            boolean nombrePostes,
            boolean dateSouhaitee,
            boolean priorite,
            boolean motif
    ) {}

    /**
     * Champs de la section II à V — Fiche de poste.
     */
    public record FicheFields(
            boolean niveauEtudes,
            boolean domaineFormation,
            boolean anneesExperience,
            boolean missionPrincipale,
            boolean activitesPrincipales,
            boolean competencesTechniques,
            boolean competencesManageriales
    ) {}
}
