package com.hrflow.roles.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "roles",
        indexes = {
                @Index(name = "idx_roles_role_name", columnList = "roleName", unique = true)
        }
)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String roleName;

    @Column(length = 300)
    private String description;

    // ========== TIMESTAMPS ==========

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // ========== LIFECYCLE ==========

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========== CONSTRUCTORS ==========

    public Role() {}

    public Role(String roleName) {
        this.roleName = roleName;
    }

    public Role(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    // ========== GETTERS / SETTERS ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    // ========== EQUALS / HASHCODE ==========

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Role role)) return false;
        return Objects.equals(id, role.id) && Objects.equals(roleName, role.roleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleName);
    }

    @Override
    public String toString() {
        return "Role{" + "id=" + id + ", roleName='" + roleName + '\'' + '}';
    }
}
