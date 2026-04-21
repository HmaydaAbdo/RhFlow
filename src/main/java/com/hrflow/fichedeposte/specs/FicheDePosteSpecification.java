package com.hrflow.fichedeposte.specs;

import com.hrflow.fichedeposte.dto.FicheDePosteSearchDto;
import com.hrflow.fichedeposte.model.FicheDePoste;
import com.hrflow.fichedeposte.model.NiveauEtudes;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class FicheDePosteSpecification {

    private FicheDePosteSpecification() {}

    public static Specification<FicheDePoste> fromSearch(FicheDePosteSearchDto search) {
        return Specification.allOf(
                intituleContains(search.intitulePoste()),
                inDirection(search.directionId()),
                hasNiveauEtudes(search.niveauEtudes())
        );
    }

    /**
     * Restreint les résultats aux fiches appartenant à l'une des directions données.
     * Si la liste est vide (directeur sans direction), aucune fiche n'est retournée.
     */
    public static Specification<FicheDePoste> inDirections(List<Long> directionIds) {
        if (directionIds == null || directionIds.isEmpty()) {
            return (root, query, cb) -> cb.disjunction(); // aucun résultat
        }
        return (root, query, cb) -> root.join("direction").get("id").in(directionIds);
    }

    private static Specification<FicheDePoste> intituleContains(String intitule) {
        if (intitule == null || intitule.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("intitulePoste")), "%" + intitule.toLowerCase() + "%");
    }

    private static Specification<FicheDePoste> inDirection(Long directionId) {
        if (directionId == null) return null;
        return (root, query, cb) ->
                cb.equal(root.join("direction").get("id"), directionId);
    }

    private static Specification<FicheDePoste> hasNiveauEtudes(NiveauEtudes niveauEtudes) {
        if (niveauEtudes == null) return null;
        return (root, query, cb) ->
                cb.equal(root.get("niveauEtudes"), niveauEtudes);
    }
}
