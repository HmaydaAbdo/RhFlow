package com.hrflow.projetrecrutement.specifications;

import com.hrflow.projetrecrutement.dto.ProjetRecrutementSearchDto;
import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import com.hrflow.projetrecrutement.model.StatutProjet;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ProjetRecrutementSpecification {

    private ProjetRecrutementSpecification() {}

    public static Specification<ProjetRecrutement> hasStatut(StatutProjet statut) {
        return (root, query, cb) ->
            statut == null ? cb.conjunction()
                           : cb.equal(root.get("statut"), statut);
    }

    public static Specification<ProjetRecrutement> forFicheDePoste(Long ficheDePosteId) {
        return (root, query, cb) ->
            ficheDePosteId == null ? cb.conjunction()
                                   : cb.equal(root.get("ficheDePoste").get("id"), ficheDePosteId);
    }

    public static Specification<ProjetRecrutement> inDirection(Long directionId) {
        return (root, query, cb) ->
            directionId == null ? cb.conjunction()
                                : cb.equal(root.get("ficheDePoste").get("direction").get("id"), directionId);
    }

    /** Restreint aux directions gérées par le directeur connecté. */
    public static Specification<ProjetRecrutement> inDirections(List<Long> directionIds) {
        return (root, query, cb) -> {
            if (directionIds == null || directionIds.isEmpty()) {
                return cb.disjunction(); // aucun résultat si aucune direction assignée
            }
            return root.get("ficheDePoste").get("direction").get("id").in(directionIds);
        };
    }

    public static Specification<ProjetRecrutement> fromSearch(ProjetRecrutementSearchDto search) {
        return hasStatut(search.statut())
            .and(forFicheDePoste(search.ficheDePosteId()))
            .and(inDirection(search.directionId()));
    }
}
