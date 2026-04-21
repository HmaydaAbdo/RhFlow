package com.hrflow.fichedeposte.dto;

import com.hrflow.fichedeposte.model.NiveauEtudes;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FicheDePosteRequest(

        @NotBlank(message = "L'intitulé du poste est obligatoire")
        String intitulePoste,

        @NotNull(message = "La direction est obligatoire")
        Long directionId,

        @NotBlank(message = "La mission principale est obligatoire")
        String missionPrincipale,

        @NotBlank(message = "Les activités principales sont obligatoires")
        String activitesPrincipales,

        @NotNull(message = "Le niveau d'études est obligatoire")
        NiveauEtudes niveauEtudes,

        @NotBlank(message = "Le domaine de formation est obligatoire")
        String domaineFormation,

        @NotNull(message = "Veuillez spécifier Les années d'expérience")
        @Min(value = 0, message = "Les années d'expérience ne peuvent pas être négatives")
        int anneesExperience,

        @NotBlank(message = "Les compétences techniques sont obligatoires")
        String competencesTechniques,

        @NotBlank(message = "Les compétences Managériales sont obligatoires")
        String competencesManageriales
) {}
