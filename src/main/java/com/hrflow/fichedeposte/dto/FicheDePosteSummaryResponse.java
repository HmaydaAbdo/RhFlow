package com.hrflow.fichedeposte.dto;

import com.hrflow.fichedeposte.model.NiveauEtudes;

import java.time.LocalDateTime;

public record FicheDePosteSummaryResponse(
        Long id,
        String intitulePoste,
        String directionNom,
        NiveauEtudes niveauEtudes,
        String domaineFormation,
        int anneesExperience,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
