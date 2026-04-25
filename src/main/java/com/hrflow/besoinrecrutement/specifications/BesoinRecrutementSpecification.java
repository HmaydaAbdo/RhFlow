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
        return Specification.allOf(
                inDirection(search.directionId()),
                forFicheDePoste(search.ficheDePosteId()),
                hasStatut(search.statut()),
                hasPriorite(search.priorite()),
                hasEncours(search.encours()
                ));
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

    /**
     * mineOnly : retourne les besoins où l'utilisateur est
     *   - le directeur de la direction (directeur_id = userId)  OU
     *   - le créateur du besoin        (created_by_id = userId)
     */
    public static Specification<BesoinRecrutement> belongsToUser(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) return cb.conjunction();
            return cb.or(
                cb.equal(root.get("directeur").get("id"), userId),
                cb.equal(root.get("createdBy").get("id"),  userId)
            );
        };
    }

    /** @deprecated Remplacé par {@link #belongsToUser(Long)} */
    public static Specification<BesoinRecrutement> belongsToDirecteur(Long directeurId) {
        return belongsToUser(directeurId);
    }

    public static Specification<BesoinRecrutement> hasEncours(Boolean encours) {
        return (root, query, cb) -> {
            if (encours == null) return cb.conjunction();
            return cb.equal(root.get("encours"), encours);
        };
    }
}
