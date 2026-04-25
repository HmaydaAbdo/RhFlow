package com.hrflow.besoinrecrutement.mapper;

import com.hrflow.besoinrecrutement.dto.BesoinRecrutementRequest;
import com.hrflow.besoinrecrutement.dto.BesoinRecrutementResponse;
import com.hrflow.besoinrecrutement.dto.BesoinRecrutementSummaryResponse;
import com.hrflow.besoinrecrutement.model.BesoinRecrutement;
import com.hrflow.besoinrecrutement.model.StatutBesoin;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BesoinRecrutementMapper {

    @Mapping(target = "ficheDePosteId",      source = "ficheDePoste.id")
    @Mapping(target = "ficheDePosteIntitule", source = "ficheDePoste.intitulePoste")
    @Mapping(target = "directionId",          source = "ficheDePoste.direction.id")
    @Mapping(target = "directionNom",         source = "ficheDePoste.direction.nom")
    @Mapping(target = "directeurId",          source = "directeur.id")
    @Mapping(target = "directeurNom",         source = "directeur.fullName")
    @Mapping(target = "createdById",          source = "createdBy.id")
    @Mapping(target = "createdByNom",         source = "createdBy.fullName")
    BesoinRecrutementResponse toResponse(BesoinRecrutement besoin);

    @Mapping(target = "ficheDePosteIntitule", source = "ficheDePoste.intitulePoste")
    @Mapping(target = "directionNom",         source = "ficheDePoste.direction.nom")
    @Mapping(target = "directeurNom",         source = "directeur.fullName")
    @Mapping(target = "createdByNom",         source = "createdBy.fullName")
    BesoinRecrutementSummaryResponse toSummary(BesoinRecrutement besoin);

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "ficheDePoste", ignore = true)
    @Mapping(target = "directeur",    ignore = true)
    @Mapping(target = "createdBy",    ignore = true)
    @Mapping(target = "encours",      ignore = true)   // positionné explicitement dans le service
    @Mapping(target = "statut",       ignore = true)   // null jusqu'à la décision DRH
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "version",      ignore = true)
    BesoinRecrutement toEntity(BesoinRecrutementRequest request);

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "ficheDePoste", ignore = true)
    @Mapping(target = "directeur",    ignore = true)
    @Mapping(target = "createdBy",    ignore = true)
    @Mapping(target = "encours",      ignore = true)
    @Mapping(target = "statut",       ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "version",      ignore = true)
    void updateEntity(BesoinRecrutementRequest request, @MappingTarget BesoinRecrutement besoin);
}
