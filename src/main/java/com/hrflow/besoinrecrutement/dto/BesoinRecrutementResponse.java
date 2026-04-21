package com.hrflow.besoinrecrutement.dto;

import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.model.StatutBesoin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BesoinRecrutementResponse(
    Long id,
    Long ficheDePosteId,
    String ficheDePosteIntitule,
    Long directionId,
    String directionNom,
    Long directeurId,
    String directeurNom,
    int nombrePostes,
    LocalDate dateSouhaitee,
    String justification,
    PrioriteBesoin priorite,
    StatutBesoin statut,
    String motifRefus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
