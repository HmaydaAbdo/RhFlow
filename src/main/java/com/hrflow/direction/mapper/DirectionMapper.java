package com.hrflow.direction.mapper;

import com.hrflow.direction.dto.DirectionBriefResponse;
import com.hrflow.direction.dto.DirectionRequest;
import com.hrflow.direction.dto.DirectionResponse;
import com.hrflow.direction.entities.Direction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DirectionMapper {

    @Mapping(target = "directeurId",  source = "directeur.id")
    @Mapping(target = "directeurNom", source = "directeur.fullName")
    @Mapping(target = "fichesDePosteCount", ignore = true)
    DirectionResponse toResponse(Direction direction);

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "directeur",   ignore = true)
    void updateEntity(DirectionRequest request, @MappingTarget Direction direction);

    @Mapping(target = "id",  source = "id")
    @Mapping(target = "nom", source = "nom")
    DirectionBriefResponse toBriefResponse(Direction direction);
}
