package com.hrflow.users.controller;

import com.hrflow.users.services.UserService;
import com.hrflow.users.dtos.*;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Users API — ADMIN only.
 */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasAnyAuthority('ADMIN','DRH')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ==========================================================================
    //  CRUD
    // ==========================================================================

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(@ModelAttribute UserSearchRequest request) {
        return ResponseEntity.ok(userService.getAllUsers(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================================================
    //  ENABLE / DISABLE
    // ==========================================================================

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<UserResponse> setUserEnabled(@PathVariable Long id,
                                                       @RequestParam boolean enabled) {
        return ResponseEntity.ok(userService.setUserEnabled(id, enabled));
    }

    // ==========================================================================
    //  ROLES
    // ==========================================================================

    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<UserResponse> addRoleToUser(@PathVariable Long userId,
                                                      @PathVariable Long roleId) {
        return ResponseEntity.ok(userService.addRoleToUser(userId, roleId));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<UserResponse> removeRoleFromUser(@PathVariable Long userId,
                                                           @PathVariable Long roleId) {
        return ResponseEntity.ok(userService.removeRoleFromUser(userId, roleId));
    }
}
