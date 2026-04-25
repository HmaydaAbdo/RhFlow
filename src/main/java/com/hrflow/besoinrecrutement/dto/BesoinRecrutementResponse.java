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
    Long createdById,
    String createdByNom,
    int nombrePostes,
    LocalDate dateSouhaitee,
    PrioriteBesoin priorite,
    boolean encours,
    StatutBesoin statut,      // null si encours=true (aucune décision encore prise)
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
