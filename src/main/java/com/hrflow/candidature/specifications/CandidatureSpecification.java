package com.hrflow.candidature.specifications;

import com.hrflow.candidature.dto.CandidatureSearchDto;
import com.hrflow.candidature.model.Candidature;
import com.hrflow.candidature.model.RecommandationIA;
import com.hrflow.candidature.model.StatutCandidature;
import org.springframework.data.jpa.domain.Specification;

public final class CandidatureSpecification {

    private CandidatureSpecification() {}

    // ── Factory depuis SearchDto ──────────────────────────────────────────────

    /**
     * Combine tous les filtres du SearchDto en une seule Specification.
     * projetId est toujours requis — c'est le scope de base de la liste.
     */
    public static Specification<Candidature> fromSearch(Long projetId,
                                                        CandidatureSearchDto search) {
        return Specification.allOf(
                byProjetId(projetId),
                byStatut(search.statut()),
                byRecommandation(search.recommandation()),
                byScoreMin(search.scoreMin())
        );
    }

    // ── Filtres individuels ───────────────────────────────────────────────────

    /**
     * Filtre obligatoire — restreint la liste aux candidatures du projet.
     * Traverse la relation ManyToOne vers ProjetRecrutement.
     */
    public static Specification<Candidature> byProjetId(Long projetId) {
        return (root, query, cb) -> {
            if (projetId == null) return cb.conjunction();
            return cb.equal(root.get("projetRecrutement").get("id"), projetId);
        };
    }

    /** Filtre optionnel sur le statut du cycle de vie de la candidature. */
    public static Specification<Candidature> byStatut(StatutCandidature statut) {
        return (root, query, cb) -> {
            if (statut == null) return cb.conjunction();
            return cb.equal(root.get("statut"), statut);
        };
    }

    /** Filtre optionnel sur la recommandation IA. */
    public static Specification<Candidature> byRecommandation(RecommandationIA recommandation) {
        return (root, query, cb) -> {
            if (recommandation == null) return cb.conjunction();
            return cb.equal(root.get("recommandation"), recommandation);
        };
    }

    /**
     * Filtre optionnel — score supérieur ou égal au minimum demandé.
     * Les candidatures sans score (NULL) sont automatiquement exclues
     * dès que scoreMin est fourni (SQL : NULL >= X est toujours faux).
     */
    public static Specification<Candidature> byScoreMin(Integer scoreMin) {
        return (root, query, cb) -> {
            if (scoreMin == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("scoreMatching"), scoreMin);
        };
    }
}
