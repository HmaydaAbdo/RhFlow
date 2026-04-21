package com.hrflow.roles.services;

import com.hrflow.roles.dtos.RoleRequest;
import com.hrflow.roles.dtos.RoleResponse;
import com.hrflow.roles.dtos.RoleSearchRequest;
import com.hrflow.roles.entities.Role;
import com.hrflow.roles.exception.RoleAlreadyExistException;
import com.hrflow.roles.exception.RoleNotFoundException;
import com.hrflow.roles.mapper.RoleMapper;
import com.hrflow.roles.repositories.RoleRepository;
import com.hrflow.roles.specs.RoleSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleMapper roleMapper;
    private final RoleRepository roleRepository;

    public RoleService(RoleMapper roleMapper, RoleRepository roleRepository) {
        this.roleMapper = roleMapper;
        this.roleRepository = roleRepository;
    }

    // ==========================================================================
    //  CRUD
    // ==========================================================================

    @Transactional
    public RoleResponse addNewRole(RoleRequest request) {
        String normalizedName = request.roleName().toUpperCase();
        log.info("Création rôle {} par acteur={}", normalizedName, currentActor());

        if (roleRepository.findByRoleName(normalizedName).isPresent()) {
            throw new RoleAlreadyExistException(normalizedName);
        }

        Role role = roleMapper.toEntity(request);
        role = roleRepository.save(role);
        log.info("Rôle créé id={} name={}", role.getId(), role.getRoleName());

        return roleMapper.toDto(role);
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        String normalizedName = request.roleName().toUpperCase();

        Optional<Role> existing = roleRepository.findByRoleName(normalizedName);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new RoleAlreadyExistException(normalizedName);
        }

        role.setRoleName(normalizedName);
        role.setDescription(request.description());

        role = roleRepository.save(role);
        log.info("Rôle mis à jour id={} name={} par acteur={}",
                role.getId(), role.getRoleName(), currentActor());
        return roleMapper.toDto(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        roleRepository.delete(role);
        log.info("Rôle supprimé id={} name={} par acteur={}",
                id, role.getRoleName(), currentActor());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        return roleMapper.toDto(role);
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> getAllRoles(RoleSearchRequest request) {
        Sort sort = request.getDirection().equalsIgnoreCase("desc")
                ? Sort.by(request.getSortBy()).descending()
                : Sort.by(request.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<Role> spec = Specification.allOf(
                RoleSpecification.roleNameContains(request.getKeyword()));

        return roleRepository.findAll(spec, pageable).map(roleMapper::toDto);
    }

    // ==========================================================================
    //  PRIVATE
    // ==========================================================================

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "system";
        }
        return authentication.getName();
    }
}
