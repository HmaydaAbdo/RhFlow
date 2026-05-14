package com.hrflow.users.repositories;

import com.hrflow.users.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Retourne le premier utilisateur ayant le rôle donné (avec ou sans signature).
     * Utilisé pour récupérer le DRH lors de l'export PDF.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    Optional<User> findFirstByRoleName(@Param("roleName") String roleName);

    /**
     * Vérifie si un autre utilisateur (différent de {@code excludeUserId}) possède déjà le rôle donné.
     * Utilisé pour garantir l'unicité du rôle DRH.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r " +
           "WHERE r.roleName = :roleName AND u.id <> :excludeUserId")
    boolean existsOtherUserWithRole(@Param("roleName") String roleName,
                                    @Param("excludeUserId") Long excludeUserId);
}
