package com.hrflow.besoinrecrutement.dto;

import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.model.StatutBesoin;

public record BesoinRecrutementSearchDto(
    Long directionId,
    Long ficheDePosteId,
    StatutBesoin statut,
    PrioriteBesoin priorite,
    /**
     * Filtre sur l'état de traitement :
     * true  → uniquement les besoins en attente de décision
     * false → uniquement les besoins décidés (acceptés ou refusés)
     * null  → tous
     */
    Boolean encours,
    /** Si true : filtre sur le directeur connecté, quel que soit son rôle. */
    boolean mineOnly
) {}
