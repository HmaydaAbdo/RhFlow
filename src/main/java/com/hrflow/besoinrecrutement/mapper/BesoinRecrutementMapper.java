package com.hrflow.besoinrecrutement.mapper;

import com.hrflow.besoinrecrutement.dto.BesoinRecrutementDetailResponse;
import com.hrflow.besoinrecrutement.dto.BesoinRecrutementRequest;
import com.hrflow.besoinrecrutement.dto.BesoinRecrutementResponse;
import com.hrflow.besoinrecrutement.dto.BesoinRecrutementSummaryResponse;
import com.hrflow.besoinrecrutement.model.BesoinRecrutement;
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

    // ── Detail (page de détail — fiche de poste complète imbriquée) ───────
    @Mapping(target = "ficheDePosteId",   source = "ficheDePoste.id")
    @Mapping(target = "directionId",      source = "ficheDePoste.direction.id")
    @Mapping(target = "directionNom",     source = "ficheDePoste.direction.nom")
    @Mapping(target = "directeurId",      source = "directeur.id")
    @Mapping(target = "directeurNom",     source = "directeur.fullName")
    @Mapping(target = "createdById",      source = "createdBy.id")
    @Mapping(target = "createdByNom",     source = "createdBy.fullName")
    @Mapping(target = "ficheDePoste",     source = "ficheDePoste")
    BesoinRecrutementDetailResponse toDetailResponse(BesoinRecrutement besoin);

    @Mapping(target = "intitulePoste",          source = "intitulePoste")
    @Mapping(target = "missionPrincipale",      source = "missionPrincipale")
    @Mapping(target = "activitesPrincipales",   source = "activitesPrincipales")
    @Mapping(target = "niveauEtudes",           source = "niveauEtudes")
    @Mapping(target = "domaineFormation",       source = "domaineFormation")
    @Mapping(target = "anneesExperience",       source = "anneesExperience")
    @Mapping(target = "competencesTechniques",  source = "competencesTechniques")
    @Mapping(target = "competencesManageriales",source = "competencesManageriales")
    BesoinRecrutementDetailResponse.FicheDePosteDetail toFicheDetail(
            com.hrflow.fichedeposte.model.FicheDePoste ficheDePoste);

    // ── Entity mapping ─────────────────────────────────────────────────────
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "ficheDePoste", ignore = true)
    @Mapping(target = "directeur",    ignore = true)
    @Mapping(target = "createdBy",    ignore = true)
    @Mapping(target = "encours",      ignore = true)
    @Mapping(target = "statut",       ignore = true)
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
