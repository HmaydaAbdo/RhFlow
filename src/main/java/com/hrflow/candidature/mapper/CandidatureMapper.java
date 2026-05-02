package com.hrflow.candidature.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrflow.candidature.dto.CandidatureResponse;
import com.hrflow.candidature.model.Candidature;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Conversion manuelle (pas MapStruct) car on doit désérialiser
 * les champs JSON TEXT (pointsForts, pointsManquants).
 */
@Component
public class CandidatureMapper {

    private final ObjectMapper objectMapper;

    public CandidatureMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CandidatureResponse toResponse(Candidature c) {
        return new CandidatureResponse(
                c.getId(),
                c.getProjetRecrutement().getId(),
                c.getProjetRecrutement().getFicheDePoste().getIntitulePoste(),
                c.getNomFichier(),
                c.getTypeFichier(),
                c.getTailleFichier(),
                c.getNomCandidat(),
                c.getEmailCandidat(),
                c.getTelephoneCandidat(),
                c.getScoreMatching(),
                parseJsonList(c.getPointsForts()),
                parseJsonList(c.getPointsManquants()),
                c.getRecommandation(),
                c.getJustificationIa(),
                c.getStatut(),
                c.getDeposeLe(),
                c.getEvalueLe()
        );
    }



    // ── Helpers ─────────────────────────────────────────────────────────────────

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
