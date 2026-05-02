package com.hrflow.candidature.dto;

import com.hrflow.candidature.model.RecommandationIA;
import com.hrflow.candidature.model.StatutCandidature;

/**
 * Paramètres de filtrage pour la liste des candidatures d'un projet.
 *
 * projetId est transmis comme path variable, pas dans ce DTO.
 * Tous les filtres sont optionnels — null = pas de filtre sur ce champ.
 */
public record CandidatureSearchDto(

        /** Filtre sur le statut du traitement IA ou de la décision RH. */
        StatutCandidature statut,

        /** Filtre sur la recommandation IA (ignoré si statut != EVALUE). */
        RecommandationIA recommandation,

        /** Score minimum inclus (0–100). Null = pas de filtre sur le score. */
        Integer scoreMin

) {}
