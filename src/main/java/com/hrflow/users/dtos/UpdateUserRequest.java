package com.hrflow.users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateUserRequest(

        @NotBlank(message = "Nom complet ne peut pas être vide")
        String fullName,

        String gsm,

        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password,

        List<Long> roleIds
) {}
