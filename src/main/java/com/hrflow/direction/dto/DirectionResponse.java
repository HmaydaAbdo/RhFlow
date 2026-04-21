package com.hrflow.direction.dto;

import java.time.LocalDateTime;

public record DirectionResponse(
        Long id,
        String nom,
        Long directeurId,
        String directeurNom,
        long fichesDePosteCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
