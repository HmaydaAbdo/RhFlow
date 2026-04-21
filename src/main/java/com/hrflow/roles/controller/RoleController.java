package com.hrflow.roles.controller;

import com.hrflow.roles.dtos.RoleRequest;
import com.hrflow.roles.dtos.RoleResponse;
import com.hrflow.roles.dtos.RoleSearchRequest;
import com.hrflow.roles.services.RoleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CRUD des rôles. Accessible uniquement à ADMIN.
 */
@RestController
@RequestMapping("/roles")
@PreAuthorize("hasAnyAuthority('ADMIN','DRH')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleResponse> addRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.addNewRole(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @GetMapping
    public ResponseEntity<Page<RoleResponse>> getAllRoles(@ModelAttribute RoleSearchRequest request) {
        return ResponseEntity.ok(roleService.getAllRoles(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
