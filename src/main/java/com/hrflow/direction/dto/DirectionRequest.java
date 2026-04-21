// src/main/java/com/hrflow/recruitment/direction/dto/DirectionRequest.java
package com.hrflow.direction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DirectionRequest(

        @NotBlank(message = "Le nom de la direction est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nom,

        @NotNull(message = "veuillez choisir un directeur ")
        Long directeurId
) {}
