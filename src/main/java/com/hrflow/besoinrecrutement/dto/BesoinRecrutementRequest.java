package com.hrflow.besoinrecrutement.dto;

import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record BesoinRecrutementRequest(

    @NotNull(message = "La fiche de poste est obligatoire")
    Long ficheDePosteId,

    @Min(value = 1, message = "Le nombre de postes doit être au moins 1")
    @Max(value = 100, message = "Le nombre de postes ne peut pas dépasser 100")
    int nombrePostes,

    @NotNull(message = "La date souhaitée est obligatoire")
    @Future(message = "La date souhaitée doit être dans le futur")
    LocalDate dateSouhaitee,

    @NotBlank(message = "La justification est obligatoire")
    @Size(min = 10, max = 2000, message = "La justification ne peut pas dépasser 2000 caractères")
    String justification,

    @NotNull(message = "La priorité est obligatoire")
    PrioriteBesoin priorite
) {}
