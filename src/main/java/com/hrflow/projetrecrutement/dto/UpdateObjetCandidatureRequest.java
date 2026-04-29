package com.hrflow.projetrecrutement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateObjetCandidatureRequest(

        @NotBlank(message = "L'objet de candidature ne peut pas être vide")
        @Size(max = 255, message = "L'objet de candidature ne peut pas dépasser 255 caractères")
        String objetCandidature
) {}
