package com.hrflow.besoinrecrutement.dto;

import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.model.StatutBesoin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BesoinRecrutementSummaryResponse(
    Long id,
    String ficheDePosteIntitule,
    String directionNom,
    String directeurNom,
    int nombrePostes,
    LocalDate dateSouhaitee,
    PrioriteBesoin priorite,
    StatutBesoin statut,
    LocalDateTime createdAt
) {}
