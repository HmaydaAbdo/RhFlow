package com.hrflow.besoinrecrutement.dto;

import com.hrflow.besoinrecrutement.model.StatutBesoin;
import jakarta.validation.constraints.NotNull;

public record DecisionBesoinRequest(

    @NotNull(message = "Le statut de décision est obligatoire")
    StatutBesoin statut

) {}
