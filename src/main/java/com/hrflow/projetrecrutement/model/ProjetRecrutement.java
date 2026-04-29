package com.hrflow.projetrecrutement.model;

import com.hrflow.besoinrecrutement.model.BesoinRecrutement;
import com.hrflow.fichedeposte.model.FicheDePoste;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "projets_recrutement",
    indexes = {
        @Index(name = "idx_projet_fiche",   columnList = "fiche_de_poste_id"),
        @Index(name = "idx_projet_statut",  columnList = "statut"),
        @Index(name = "idx_projet_besoin",  columnList = "besoin_recrutement_id")
    }
)
public class ProjetRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "besoin_recrutement_id", nullable = false, unique = true)
    private BesoinRecrutement besoinRecrutement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiche_de_poste_id", nullable = false)
    private FicheDePoste ficheDePoste;

    @Column(nullable = false)
    private int nombrePostes;

    /** Objet de mail unique que les candidats doivent utiliser pour postuler. */
    @Column(name = "objet_candidature", nullable = false, unique = true, length = 255)
    private String objetCandidature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatutProjet statut;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime closedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    public ProjetRecrutement() {}

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.statut == null) {
            this.statut = StatutProjet.OUVERT;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BesoinRecrutement getBesoinRecrutement() { return besoinRecrutement; }
    public void setBesoinRecrutement(BesoinRecrutement besoinRecrutement) { this.besoinRecrutement = besoinRecrutement; }

    public FicheDePoste getFicheDePoste() { return ficheDePoste; }
    public void setFicheDePoste(FicheDePoste ficheDePoste) { this.ficheDePoste = ficheDePoste; }

    public int getNombrePostes() { return nombrePostes; }
    public void setNombrePostes(int nombrePostes) { this.nombrePostes = nombrePostes; }

    public String getObjetCandidature() { return objetCandidature; }
    public void setObjetCandidature(String objetCandidature) { this.objetCandidature = objetCandidature; }

    public StatutProjet getStatut() { return statut; }
    public void setStatut(StatutProjet statut) { this.statut = statut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
