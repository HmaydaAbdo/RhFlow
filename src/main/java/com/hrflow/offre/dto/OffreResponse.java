package com.hrflow.offre.dto;

import java.time.LocalDateTime;

public record OffreResponse(
    Long           id,
    String         contenu,

    Long           projetRecrutementId,
    String         ficheDePosteIntitule,
    String         objetCandidature,

    LocalDateTime  generatedAt,
    LocalDateTime  updatedAt
) {}
