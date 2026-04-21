package com.hrflow.users.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(

        @NotBlank(message = "L'email ne peut pas être vide")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Nom complet ne peut pas être vide")
        String fullName,

        @NotBlank(message = "GSM ne peut pas être vide ")
        @NotNull(message = " ")
        String gsm,

        @NotBlank(message = "Mot de passe ne peut pas être vide")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password,

        List<Long> roleIds
) {}
