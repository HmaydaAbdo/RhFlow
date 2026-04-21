package com.hrflow.roles.dtos;

import java.time.LocalDateTime;

/**
 * Réponse rôle. Champs d'audit inclus pour le master-detail du front.
 */
public record RoleResponse(
        Long roleId,
        String roleName,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
