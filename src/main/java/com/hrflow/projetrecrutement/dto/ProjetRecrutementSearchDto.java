package com.hrflow.projetrecrutement.dto;

import com.hrflow.projetrecrutement.model.StatutProjet;

public record ProjetRecrutementSearchDto(
    Long         directionId,
    Long         ficheDePosteId,
    StatutProjet statut
) {}
