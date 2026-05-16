package com.hrflow.ai.dto;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

/**
 * Informations extraites d'un CV par l'IA.
 *
 * Tous les champs scalaires sont nullable : si l'IA ne trouve pas l'information,
 * elle retourne null plutôt que d'inventer une valeur. Les listes
 * {@code formations} et {@code experiences} sont toujours présentes — vides
 * lorsque le CV ne contient pas la rubrique correspondante.
 */
@Description("Informations extraites d'un CV : coordonnées, formations et expériences professionnelles")
public record CandidatInfo(

        @Description("Prénom + nom complet du candidat, tel qu'écrit dans le CV. " +
                "Exemple : \"Mohamed El Idrissi\". null si introuvable.")
        String nomCandidat,

        @Description("Adresse email de contact telle qu'identifiable dans le texte, " +
                "espaces parasites ignorés. Exemple : \"mohamed.elidrissi@example.com\". " +
                "null si introuvable.")
        String emailCandidat,

        @Description("Numéro de téléphone, conservé dans le format ORIGINAL du CV " +
                "(avec indicatif si présent). Exemple : \"+212 6 12 34 56 78\". null si introuvable.")
        String telephoneCandidat,

        @Description("Liste des formations / diplômes du candidat. Liste vide si le CV " +
                "ne contient aucune rubrique formation.")
        List<Formation> formations,

        @Description("Liste des expériences professionnelles du candidat, ordre du CV " +
                "préservé. Liste vide si le CV ne contient aucune rubrique expérience.")
        List<ExperienceProfessionnelle> experiences
) {}
