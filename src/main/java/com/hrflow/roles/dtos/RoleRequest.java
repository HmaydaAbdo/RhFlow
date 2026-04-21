package com.hrflow.roles.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour création/édition d'un rôle.
 */
public record RoleRequest(
        @NotBlank(message = "Le nom du rôle ne peut pas être vide")
        @Size(min = 2, max = 50, message = "Le nom du rôle doit contenir entre 2 et 50 caractères")
        String roleName,

        @Size(max = 300, message = "La description ne peut pas dépasser 300 caractères")
        String description
) {}
