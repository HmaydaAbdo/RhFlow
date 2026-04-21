package com.hrflow.projetrecrutement.dto;

import com.hrflow.projetrecrutement.model.StatutProjet;

import java.time.LocalDateTime;

public record ProjetRecrutementResponse(
    Long             id,
    StatutProjet     statut,
    int              nombrePostes,

    Long             ficheDePosteId,
    String           ficheDePosteIntitule,

    Long             directionId,
    String           directionNom,

    String           directeurNom,

    Long             besoinRecrutementId,

    LocalDateTime    createdAt,
    LocalDateTime    updatedAt,
    LocalDateTime    closedAt
) {}
