package com.hrflow.users.repositories;

import com.hrflow.users.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Fetch paginé avec rôles — évite N+1 dans getAllUsers(). */
    @EntityGraph(attributePaths = "roles")
    @Override
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    /**
     * Charge le user et ses rôles en une seule requête (évite N+1 au login).
     */
    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesByEmail(String email);

    /**
     * Charge un user avec ses rôles par id — utilisé par les opérations
     * de gestion des rôles d'un user pour éviter un LazyInitializationException.
     */
    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesById(Long id);
}
