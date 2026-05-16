package com.hrflow.ai.dto;

import dev.langchain4j.model.output.structured.Description;

/**
 * Expérience professionnelle extraite d'un CV par l'IA.
 *
 * Tous les champs sont nullable : si l'IA ne trouve pas l'information,
 * elle retourne null plutôt que d'inventer une valeur.
 */
@Description("Une expérience professionnelle (poste occupé) listée dans le CV")
public record ExperienceProfessionnelle(

        @Description("Intitulé du poste occupé, tel qu'écrit dans le CV. " +
                "Exemple : \"Développeur Full Stack\", \"Chef de Projet IT\". null si non précisé.")
        String titre,

        @Description("Nom de l'entreprise ou de l'organisation. " +
                "Exemple : \"OCP Group\", \"Capgemini\". null si non précisé.")
        String entreprise,

        @Description("Période d'occupation, conservée EN TEXTE BRUT, dans le format exact du CV. " +
                "Exemples : \"2019 - 2021\", \"Sept. 2019 – Juin 2021\", \"depuis 2022\". " +
                "Ne pas reformater, ne pas convertir. null si non précisée.")
        String periode
) {}
