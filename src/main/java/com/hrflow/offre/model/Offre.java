package com.hrflow.offre.model;

import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "offres",
    indexes = {
        @Index(name = "idx_offre_projet", columnList = "projet_recrutement_id")
    }
)
public class Offre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Le projet de recrutement auquel cette offre est rattachée (1:1). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_recrutement_id", nullable = false, unique = true)
    private ProjetRecrutement projetRecrutement;

    /** Contenu Markdown généré par l'IA (ou édité manuellement). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    public Offre() {}

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.generatedAt = now;
        this.updatedAt   = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProjetRecrutement getProjetRecrutement() { return projetRecrutement; }
    public void setProjetRecrutement(ProjetRecrutement projetRecrutement) { this.projetRecrutement = projetRecrutement; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
