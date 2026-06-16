package com.hrflow.ingestion.mapper;

import com.hrflow.ingestion.dto.IngestionRecordResponse;
import com.hrflow.ingestion.model.IngestionRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Convertit un {@link IngestionRecord} (entité JPA) en
 * {@link IngestionRecordResponse} (DTO public).
 *
 * <p>{@code componentModel = "spring"} → MapStruct génère une implémentation
 * annotée {@code @Component}, injectable comme n'importe quel bean.
 *
 * <p>La quasi-totalité des champs ont le même nom des deux côtés, donc
 * MapStruct les mappe automatiquement. Seul exception : {@code candidatureId}
 * — on l'extrait via le chemin {@code candidature.id} sur l'entité.
 *
 * <p>Comment vérifier la classe générée : après compilation, regarde
 * {@code target/generated-sources/annotations/com/hrflow/ingestion/mapper/IngestionRecordMapperImpl.java}.
 * C'est du code Java standard, pas de magie runtime.
 */
@Mapper(componentModel = "spring")
public interface IngestionRecordMapper {

    @Mapping(target = "candidatureId",       source = "candidature.id")
    @Mapping(target = "projetRecrutementId", source = "candidature.projetRecrutement.id")
    IngestionRecordResponse toResponse(IngestionRecord record);
}
