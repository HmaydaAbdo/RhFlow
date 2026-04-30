package com.hrflow.offre.mapper;

import com.hrflow.offre.dto.OffreResponse;
import com.hrflow.offre.dto.OffreSummaryResponse;
import com.hrflow.offre.model.Offre;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OffreMapper {

    @Mapping(target = "projetRecrutementId",  source = "projetRecrutement.id")
    @Mapping(target = "ficheDePosteIntitule", source = "projetRecrutement.ficheDePoste.intitulePoste")
    @Mapping(target = "objetCandidature",     source = "projetRecrutement.objetCandidature")
    OffreResponse toResponse(Offre offre);

    OffreSummaryResponse toSummary(Offre offre);
}
