package com.hrflow.candidature.dto;

import com.hrflow.ai.dto.ExperienceProfessionnelle;
import com.hrflow.ai.dto.Formation;
import com.hrflow.candidature.model.RecommandationIA;
import com.hrflow.candidature.model.StatutCandidature;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse complète d'une candidature (détail + liste).
 * {@code pointsForts}, {@code pointsManquants}, {@code questionsEntretien},
 * {@code formations} et {@code experiences} sont désérialisés depuis JSON TEXT.
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

        // Parcours candidat (extrait par IA)
        List<Formation>                 formations,
        List<ExperienceProfessionnelle> experiences,

        // Évaluation IA
        Integer           scoreMatching,
        List<String>      pointsForts,
        List<String>      pointsManquants,
        RecommandationIA  recommandation,
        String            justificationIa,

        // Questions d'entretien générées par l'IA
        List<String>      questionsEntretien,

        // Statut & dates
        StatutCandidature statut,
        LocalDateTime     deposeLe,
        LocalDateTime     evalueLe
) {}
