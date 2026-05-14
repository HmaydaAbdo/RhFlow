package com.hrflow.projetrecrutement.dto;

/**
 * Sélection des champs à inclure dans l'export PDF d'un projet de recrutement.
 * Le document est destiné à la Direction Générale pour validation.
 *
 * L'intitulé du poste et l'objet de candidature sont toujours inclus (titre + référence).
 * Chaque booléen contrôle l'inclusion du champ dans le texte narratif.
 */
public record ProjetPdfExportRequest(

        ContexteFields  contexte,
        FicheFields     fiche

) {

    /**
     * Champs de la section I — Présentation de la demande.
     * Puisés dans le besoin de recrutement lié.
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
