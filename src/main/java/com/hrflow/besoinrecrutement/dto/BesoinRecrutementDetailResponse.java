package com.hrflow.besoinrecrutement.dto;

import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.model.StatutBesoin;
import com.hrflow.fichedeposte.model.NiveauEtudes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Réponse enrichie pour la page de détail d'un besoin de recrutement.
 * Contient toutes les informations du besoin + les détails complets de la fiche de poste associée.
 * Utilisé uniquement par GET /besoins-recrutement/{id}.
 */
public record BesoinRecrutementDetailResponse(

        // ── Besoin ────────────────────────────────────────────────────────
        Long    id,
        Long    ficheDePosteId,
        Long    directionId,
        String  directionNom,
        Long    directeurId,
        String  directeurNom,
        Long    createdById,
        String  createdByNom,
        String  lieuAffectation,
        String  motif,
        int     nombrePostes,
        LocalDate dateSouhaitee,
        PrioriteBesoin priorite,
        boolean encours,
        StatutBesoin   statut,
        LocalDateTime  createdAt,
        LocalDateTime  updatedAt,

        // ── Fiche de poste (détail complet) ───────────────────────────────
        FicheDePosteDetail ficheDePoste

) {
    /**
     * Détail de la fiche de poste imbriqué dans la réponse.
     * Séparé en record interne pour clarté dans le JSON et la sérialisation.
     */
    public record FicheDePosteDetail(
            String       intitulePoste,
            String       missionPrincipale,
            String       activitesPrincipales,
            NiveauEtudes niveauEtudes,
            String       domaineFormation,
            int          anneesExperience,
            String       competencesTechniques,
            String       competencesManageriales
    ) {}
}
