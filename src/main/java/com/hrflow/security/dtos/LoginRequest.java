package com.hrflow.security.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "L'email ne peut pas être vide")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Mot de passe ne peut pas être vide")
        String password
) {}
