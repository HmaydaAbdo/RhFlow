package com.hrflow.ai.dto;

import dev.langchain4j.model.output.structured.Description;

/**
 * Formation (diplôme, certification) extraite d'un CV par l'IA.
 *
 * Tous les champs sont nullable : si l'IA ne trouve pas l'information,
 * elle retourne null plutôt que d'inventer une valeur.
 */
@Description("Une formation (diplôme, certification) listée dans le CV")
public record Formation(

        @Description("Intitulé de la formation ou du diplôme, tel qu'écrit dans le CV. " +
                "Exemple : \"Ingénieur d'État en Informatique\". null si non précisé.")
        String titre,

        @Description("Établissement de formation (école, université, organisme), tel qu'écrit dans le CV. " +
                "Exemple : \"ENSIAS\", \"Université Mohammed V\". null si non précisé.")
        String ecole,

        @Description("Date ou année d'obtention, conservée EN TEXTE BRUT, dans le format exact du CV. " +
                "Exemples : \"2020\", \"Juin 2021\", \"2018-2021\". " +
                "Ne pas reformater, ne pas convertir. null si non précisée.")
        String dateObtention
) {}
