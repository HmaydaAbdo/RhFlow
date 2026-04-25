package com.hrflow.users.services;

import com.hrflow.roles.entities.Role;
import com.hrflow.users.dtos.UpdateUserRequest;
import com.hrflow.users.entities.User;
import com.hrflow.roles.exception.RoleNotFoundException;
import com.hrflow.users.exception.UserAlreadyExistException;
import com.hrflow.users.exception.UserNotFoundException;
import com.hrflow.users.exception.UserRoleException;
import com.hrflow.users.mapper.UserMapper;
import com.hrflow.roles.repositories.RoleRepository;
import com.hrflow.users.repositories.UserRepository;
import com.hrflow.users.specs.UserSpecification;
import com.hrflow.users.dtos.CreateUserRequest;
import com.hrflow.users.dtos.UserResponse;
import com.hrflow.users.dtos.UserSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    // ==========================================================================
    //  CRUD
    // ==========================================================================

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.info("Création utilisateur email={} par acteur={}", normalizedEmail, currentActor());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistException(normalizedEmail);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName());
        user.setGsm(request.gsm());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(request.roleIds());

            if (roles.size() != request.roleIds().size()) {
                Set<Long> foundIds = new HashSet<>();
                for (Role r : roles) {
                    foundIds.add(r.getId());
                }
                Long missing = request.roleIds().stream()
                        .filter(id -> !foundIds.contains(id))
                        .findFirst()
                        .orElse(null);
                throw new RoleNotFoundException(missing);
            }
            user.setRoles(roles);
        }

        user = userRepository.save(user);
        log.info("Utilisateur créé id={} email={}", user.getId(), user.getEmail());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setFullName(request.fullName());
        user.setGsm(request.gsm());

        // Changer le mot de passe seulement si fourni
        if (StringUtils.hasText(request.password())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        // Remplacer les rôles seulement si fournis
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(request.roleIds());
            if (roles.size() != request.roleIds().size()) {
                throw new RoleNotFoundException(null);
            }
            user.setRoles(roles);
        }

        user = userRepository.save(user);
        log.info("Utilisateur mis à jour id={} par acteur={}", id, currentActor());
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(UserSearchRequest request) {
        Sort sort = request.getDirection().equalsIgnoreCase("desc")
                ? Sort.by(request.getSortBy()).descending()
                : Sort.by(request.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<User> spec = Specification.allOf(
                UserSpecification.keywordContains(request.getKeyword()),
                UserSpecification.isEnabled(request.getEnabled()),
                UserSpecification.hasRole(request.getRole())
        );

        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toDto(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
        log.info("Utilisateur supprimé id={} email={} par acteur={}",
                id, user.getEmail(), currentActor());
    }

    // ==========================================================================
    //  ENABLE / DISABLE
    // ==========================================================================

    @Transactional
    public UserResponse setUserEnabled(Long id, boolean enabled) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setEnabled(enabled);
        user = userRepository.save(user);
        log.info("Utilisateur id={} email={} enabled={} par acteur={}",
                id, user.getEmail(), enabled, currentActor());
        return userMapper.toDto(user);
    }

    // ==========================================================================
    //  ROLE MANAGEMENT (assign / unassign roles à un user)
    // ==========================================================================

    @Transactional
    public UserResponse addRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        if (user.getRoles().contains(role)) {
            throw UserRoleException.alreadyAssigned(userId, roleId);
        }
        user.getRoles().add(role);
        user = userRepository.save(user);
        log.info("Rôle {} ajouté au user id={} par acteur={}",
                role.getRoleName(), userId, currentActor());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserResponse removeRoleFromUser(Long userId, Long roleId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        if (!user.getRoles().contains(role)) {
            throw UserRoleException.notAssigned(userId, roleId);
        }
        user.getRoles().remove(role);
        user = userRepository.save(user);
        log.info("Rôle {} retiré du user id={} par acteur={}",
                role.getRoleName(), userId, currentActor());
        return userMapper.toDto(user);
    }

    // ==========================================================================
    //  PRIVATE
    // ==========================================================================

    /**
     * Retourne l'identifiant (email) de l'acteur courant, ou "system"
     * si aucune authentification n'est présente (jobs, seeders, etc.).
     * Utilisé pour les logs de traçabilité.
     */
    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "system";
        }
        return authentication.getName();
    }

    private static String normalizeEmail(String rawEmail) {
        if (!StringUtils.hasText(rawEmail)) {
            return rawEmail;
        }
        return rawEmail.trim().toLowerCase();
    }
}
