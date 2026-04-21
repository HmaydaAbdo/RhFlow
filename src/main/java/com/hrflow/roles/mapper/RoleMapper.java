package com.hrflow.roles.mapper;

import com.hrflow.roles.dtos.RoleRequest;
import com.hrflow.roles.dtos.RoleResponse;
import com.hrflow.roles.entities.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public Role toEntity(RoleRequest request) {
        Role role = new Role(request.roleName().toUpperCase());
        role.setDescription(request.description());
        return role;
    }

    public RoleResponse toDto(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getRoleName(),
                role.getDescription(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}
