package com.hrflow.fichedeposte.dto;

import com.hrflow.direction.dto.DirectionBriefResponse;
import com.hrflow.fichedeposte.model.NiveauEtudes;

import java.time.LocalDateTime;

public record FicheDePosteResponse(
        Long id,
        String intitulePoste,
        DirectionBriefResponse direction,
        String missionPrincipale,
        String activitesPrincipales,
        NiveauEtudes niveauEtudes,
        String domaineFormation,
        int anneesExperience,
        String competencesTechniques,
        String competencesManageriales,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
