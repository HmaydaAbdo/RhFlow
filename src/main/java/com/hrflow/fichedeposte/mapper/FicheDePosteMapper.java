package com.hrflow.fichedeposte.mapper;

import com.hrflow.direction.mapper.DirectionMapper;
import com.hrflow.fichedeposte.dto.FicheDePosteRequest;
import com.hrflow.fichedeposte.dto.FicheDePosteResponse;
import com.hrflow.fichedeposte.dto.FicheDePosteSummaryResponse;
import com.hrflow.fichedeposte.model.FicheDePoste;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = DirectionMapper.class)
public interface FicheDePosteMapper {

    @Mapping(target = "direction", source = "direction")
    FicheDePosteResponse toResponse(FicheDePoste ficheDePoste);

    @Mapping(target = "directionNom", source = "direction.nom")
    FicheDePosteSummaryResponse toSummary(FicheDePoste ficheDePoste);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "direction", ignore = true)
    FicheDePoste toEntity(FicheDePosteRequest request);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "direction", ignore = true)
    void updateEntity(FicheDePosteRequest request, @MappingTarget FicheDePoste ficheDePoste);
}
