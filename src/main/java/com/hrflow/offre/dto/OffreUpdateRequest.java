package com.hrflow.offre.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OffreUpdateRequest(
    @NotBlank(message = "Le contenu de l'offre ne peut pas être vide")
    @Size(min = 50, message = "Le contenu doit contenir au moins 50 caractères")
    String contenu
) {}
