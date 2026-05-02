package com.hrflow.ai.dto;

/**
 * Informations de contact extraites d'un CV par l'IA.
 *
 * Tous les champs sont nullable : si l'IA ne trouve pas l'information,
 * elle retourne null plutôt que d'inventer une valeur.
 */
public record CandidatInfo(
        String nomCandidat,
        String emailCandidat,
        String telephoneCandidat
) {}
