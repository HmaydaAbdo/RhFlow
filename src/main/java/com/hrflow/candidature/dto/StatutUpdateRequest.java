package com.hrflow.candidature.dto;

import com.hrflow.candidature.model.StatutCandidature;
import jakarta.validation.constraints.NotNull;

/**
 * Payload pour le PATCH /candidatures/{id}/statut.
 * Seuls RETENU et REJETE sont acceptés (changements RH manuels).
 */
public record StatutUpdateRequest(
        @NotNull StatutCandidature statut
) {}
