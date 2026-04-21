package com.hrflow.projetrecrutement.mapper;

import com.hrflow.projetrecrutement.dto.ProjetRecrutementResponse;
import com.hrflow.projetrecrutement.dto.ProjetRecrutementSummaryResponse;
import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjetRecrutementMapper {

    @Mapping(target = "ficheDePosteId",      source = "ficheDePoste.id")
    @Mapping(target = "ficheDePosteIntitule", source = "ficheDePoste.intitulePoste")
    @Mapping(target = "directionId",          source = "ficheDePoste.direction.id")
    @Mapping(target = "directionNom",         source = "ficheDePoste.direction.nom")
    @Mapping(target = "directeurNom",         source = "ficheDePoste.direction.directeur.fullName")
    @Mapping(target = "besoinRecrutementId",  source = "besoinRecrutement.id")
    ProjetRecrutementResponse toResponse(ProjetRecrutement projet);

    @Mapping(target = "ficheDePosteIntitule", source = "ficheDePoste.intitulePoste")
    @Mapping(target = "directionNom",         source = "ficheDePoste.direction.nom")
    @Mapping(target = "directeurNom",         source = "ficheDePoste.direction.directeur.fullName")
    ProjetRecrutementSummaryResponse toSummary(ProjetRecrutement projet);
}
