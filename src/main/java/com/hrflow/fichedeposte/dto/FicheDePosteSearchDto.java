package com.hrflow.fichedeposte.dto;

import com.hrflow.fichedeposte.model.NiveauEtudes;

public record FicheDePosteSearchDto(
        String intitulePoste,
        Long directionId,
        NiveauEtudes niveauEtudes
) {}
