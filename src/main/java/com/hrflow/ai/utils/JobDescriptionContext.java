package com.hrflow.ai.utils;

import com.hrflow.ai.config.AiFallbackProperties.CompanyProfile;
import com.hrflow.besoinrecrutement.model.BesoinRecrutement;
import com.hrflow.fichedeposte.model.FicheDePoste;

import java.time.LocalDate;
import java.time.Year;

public record JobDescriptionContext(
        // ── Company (from config, not hardcoded) ──────────────────────────────
        String companyNom,
        String companySecteur,
        String companyVille,
        int    companyAnneeFondation,
        String companyEmail,
        // ── Job description ───────────────────────────────────────────────────
        String intitulePoste,
        String directionNom,
        String missionPrincipale,
        String activitesPrincipales,
        String competencesTechniques,
        String competencesManageriales,
        String niveauEtudes,
        String domaineFormation,
        int    anneesExperience,
        int    nombrePostes,
        String priorite,
        LocalDate dateSouhaitee
) {
    public static JobDescriptionContext from(BesoinRecrutement besoin, CompanyProfile company) {
        FicheDePoste fiche = besoin.getFicheDePoste();
        return new JobDescriptionContext(
                company.nom(),
                company.secteur(),
                company.ville(),
                company.anneeFondation(),
                company.emailRecrutement(),
                fiche.getIntitulePoste(),
                fiche.getDirection().getNom(),
                fiche.getMissionPrincipale(),
                fiche.getActivitesPrincipales(),
                fiche.getCompetencesTechniques(),
                fiche.getCompetencesManageriales(),
                fiche.getNiveauEtudes().name(),
                fiche.getDomaineFormation(),
                fiche.getAnneesExperience(),
                besoin.getNombrePostes(),
                besoin.getPriorite().name(),
                besoin.getDateSouhaitee()
        );
    }

    public String toPromptText() {
        int ansFondation = Year.now().getValue() - companyAnneeFondation;
        return """

            ## Entreprise
            - **Nom**               : %s
            - **Secteur**           : %s
            - **Localisation**      : %s
            - **Fondée en**         : %d (%d ans d'expérience)
            - **Email recrutement** : %s

            ## Fiche de poste
            - **Intitulé**               : %s
            - **Direction**              : %s
            - **Mission principale**     : %s
            - **Activités**              : %s
            - **Compétences techniques** : %s
            - **Compétences managériales**: %s
            - **Niveau d'études**        : %s
            - **Domaine de formation**   : %s
            - **Expérience requise**     : %d an(s)

            ## Besoin de recrutement
            - **Nombre de postes** : %d
            - **Priorité**         : %s
            - **Prise de poste**   : %s
            """.formatted(
                companyNom, companySecteur, companyVille,
                companyAnneeFondation, ansFondation, companyEmail,
                intitulePoste, directionNom, missionPrincipale,
                activitesPrincipales, competencesTechniques,
                competencesManageriales, niveauEtudes,
                domaineFormation, anneesExperience,
                nombrePostes, priorite, dateSouhaitee
        );
    }
}