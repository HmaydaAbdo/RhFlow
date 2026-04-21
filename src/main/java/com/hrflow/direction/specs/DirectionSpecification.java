// src/main/java/com/hrflow/recruitment/direction/DirectionSpecification.java
package com.hrflow.direction.specs;

import com.hrflow.direction.dto.DirectionSearchDto;
import com.hrflow.direction.entities.Direction;
import org.springframework.data.jpa.domain.Specification;

public class DirectionSpecification {

    private DirectionSpecification() {}

    public static Specification<Direction> fromSearch(DirectionSearchDto search) {
        return Specification.allOf(nomContains(
                search.nom()),
                hasDirecteur(search.directeurId())
        )
                ;
    }

    private static Specification<Direction> nomContains(String nom) {
        return (root, q, cb) -> nom == null || nom.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("nom")), "%" + nom.toLowerCase() + "%");
    }

    private static Specification<Direction> hasDirecteur(Long directeurId) {
        return (root, q, cb) -> directeurId == null
                ? cb.conjunction()
                : cb.equal(root.get("directeur").get("id"), directeurId);
    }
}
