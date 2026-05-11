package com.hrflow.besoinrecrutement.dto;

import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record BesoinRecrutementRequest(

    @NotNull(message = "La fiche de poste est obligatoire")
    Long ficheDePosteId,

    @NotBlank(message = "Le lieu d'affectation est obligatoire")
    @Size(max = 200, message = "Le lieu d'affectation ne peut pas dépasser 200 caractères")
    String lieuAffectation,

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 2000, message = "Le motif ne peut pas dépasser 2000 caractères")
    String motif,

    @Min(value = 1, message = "Le nombre de postes doit être au moins 1")
    @Max(value = 100, message = "Le nombre de postes ne peut pas dépasser 100")
    int nombrePostes,

    @NotNull(message = "La date souhaitée est obligatoire")
    @Future(message = "La date souhaitée doit être dans le futur")
    LocalDate dateSouhaitee,

    @NotNull(message = "La priorité est obligatoire")
    PrioriteBesoin priorite
) {}
