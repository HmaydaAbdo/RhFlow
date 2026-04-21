package com.hrflow.besoinrecrutement.dto;

import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.model.StatutBesoin;

public record BesoinRecrutementSearchDto(
    Long directionId,
    Long ficheDePosteId,
    StatutBesoin statut,
    PrioriteBesoin priorite,
    /** Si true : filtre sur le directeur connecté, quel que soit son rôle. */
    boolean mineOnly
) {}
