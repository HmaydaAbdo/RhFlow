package com.hrflow.projetrecrutement.dto;

import com.hrflow.projetrecrutement.model.StatutProjet;

import java.time.LocalDateTime;

public record ProjetRecrutementSummaryResponse(
    Long          id,
    StatutProjet  statut,
    int           nombrePostes,
    String        objetCandidature,
    String        ficheDePosteIntitule,
    String        directionNom,
    String        directeurNom,
    LocalDateTime createdAt,
    LocalDateTime closedAt
) {}
