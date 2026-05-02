package com.hrflow.candidature.dto;

import com.hrflow.candidature.model.RecommandationIA;
import com.hrflow.candidature.model.StatutCandidature;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse complète d'une candidature (détail + liste).
 * {@code pointsForts} et {@code pointsManquants} sont désérialisés depuis JSON TEXT.
 */
public record CandidatureResponse(
        Long              id,
        Long              projetRecrutementId,
        String            nomPoste,

        // Fichier
        String            nomFichier,
        String            typeFichier,
        Long              tailleFichier,

        // Identité candidat (extraite par IA)
        String            nomCandidat,
        String            emailCandidat,
        String            telephoneCandidat,

        // Évaluation IA
        Integer           scoreMatching,
        List<String>      pointsForts,
        List<String>      pointsManquants,
        RecommandationIA  recommandation,
        String            justificationIa,

        // Statut & dates
        StatutCandidature statut,
        LocalDateTime     deposeLe,
        LocalDateTime     evalueLe
) {}
