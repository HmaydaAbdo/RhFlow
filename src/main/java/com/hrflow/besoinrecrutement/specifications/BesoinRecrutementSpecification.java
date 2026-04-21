package com.hrflow.besoinrecrutement.specifications;

import com.hrflow.besoinrecrutement.dto.BesoinRecrutementSearchDto;
import com.hrflow.besoinrecrutement.model.BesoinRecrutement;
import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.model.StatutBesoin;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class BesoinRecrutementSpecification {

    private BesoinRecrutementSpecification() {}

    // ---- Factory depuis SearchDto ----
    public static Specification<BesoinRecrutement> fromSearch(BesoinRecrutementSearchDto search) {
        return Specification
                .where(inDirection(search.directionId()))
                .and(forFicheDePoste(search.ficheDePosteId()))
                .and(hasStatut(search.statut()))
                .and(hasPriorite(search.priorite()));
    }

    // ---- Restreindre à une liste de directions (usage DIRECTEUR) ----
    public static Specification<BesoinRecrutement> inDirections(List<Long> directionIds) {
        return (root, query, cb) -> {
            if (directionIds == null || directionIds.isEmpty()) {
                return cb.disjunction(); // aucune direction → aucun résultat
            }
            var fiche = root.join("ficheDePoste", JoinType.INNER);
            return fiche.get("direction").get("id").in(directionIds);
        };
    }

    // ---- Filtres individuels ----
    public static Specification<BesoinRecrutement> inDirection(Long directionId) {
        return (root, query, cb) -> {
            if (directionId == null) return cb.conjunction();
            var fiche = root.join("ficheDePoste", JoinType.INNER);
            return cb.equal(fiche.get("direction").get("id"), directionId);
        };
    }

    public static Specification<BesoinRecrutement> forFicheDePoste(Long ficheDePosteId) {
        return (root, query, cb) -> {
            if (ficheDePosteId == null) return cb.conjunction();
            return cb.equal(root.get("ficheDePoste").get("id"), ficheDePosteId);
        };
    }

    public static Specification<BesoinRecrutement> hasStatut(StatutBesoin statut) {
        return (root, query, cb) -> {
            if (statut == null) return cb.conjunction();
            return cb.equal(root.get("statut"), statut);
        };
    }

    public static Specification<BesoinRecrutement> hasPriorite(PrioriteBesoin priorite) {
        return (root, query, cb) -> {
            if (priorite == null) return cb.conjunction();
            return cb.equal(root.get("priorite"), priorite);
        };
    }

    public static Specification<BesoinRecrutement> belongsToDirecteur(Long directeurId) {
        return (root, query, cb) -> {
            if (directeurId == null) return cb.conjunction();
            return cb.equal(root.get("directeur").get("id"), directeurId);
        };
    }
}
