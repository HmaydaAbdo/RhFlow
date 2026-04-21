package com.hrflow.roles.specs;

import com.hrflow.roles.entities.Role;
import org.springframework.data.jpa.domain.Specification;

public class RoleSpecification {

    public static Specification<Role> roleNameContains(String keyword) {
        return (root, query, cb) ->
                keyword == null || keyword.isBlank() ? cb.conjunction()
                        : cb.like(cb.lower(root.get("roleName")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Role> roleNameEquals(String roleName) {
        return (root, query, cb) ->
                roleName == null
                        ? cb.conjunction()
                        : cb.equal(cb.lower(root.get("roleName")), roleName.toLowerCase());
    }


}