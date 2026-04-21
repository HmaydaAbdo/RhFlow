package com.hrflow.besoinrecrutement.dto;

import com.hrflow.besoinrecrutement.model.StatutBesoin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionBesoinRequest(

    @NotNull(message = "Le statut de décision est obligatoire")
    StatutBesoin statut,

    @Size(max = 1000, message = "Le motif de refus ne peut pas dépasser 1000 caractères")
    String motifRefus
) {
    @AssertTrue(message = "Le statut doit être ACCEPTE ou REFUSE")
    public boolean isStatutValid() {
        return statut == null
            || statut == StatutBesoin.ACCEPTE
            || statut == StatutBesoin.REFUSE;
    }
}
