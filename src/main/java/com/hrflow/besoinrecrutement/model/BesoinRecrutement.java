package com.hrflow.besoinrecrutement.model;

import com.hrflow.fichedeposte.model.FicheDePoste;
import com.hrflow.users.entities.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "besoins_recrutement",
    indexes = {
        @Index(name = "idx_besoin_fiche_statut",  columnList = "fiche_de_poste_id, statut"),
        @Index(name = "idx_besoin_directeur",      columnList = "directeur_id"),
        @Index(name = "idx_besoin_statut",         columnList = "statut")
    }
)
public class BesoinRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiche_de_poste_id", nullable = false)
    private FicheDePoste ficheDePoste;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "directeur_id", nullable = false)
    private User directeur;

    @Column(nullable = false)
    private int nombrePostes;

    @Column(nullable = false)
    private LocalDate dateSouhaitee;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioriteBesoin priorite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatutBesoin statut;

    @Column(columnDefinition = "TEXT")
    private String motifRefus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    public BesoinRecrutement() {}

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.statut == null) {
            this.statut = StatutBesoin.EN_COURS;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FicheDePoste getFicheDePoste() { return ficheDePoste; }
    public void setFicheDePoste(FicheDePoste ficheDePoste) { this.ficheDePoste = ficheDePoste; }

    public User getDirecteur() { return directeur; }
    public void setDirecteur(User directeur) { this.directeur = directeur; }

    public int getNombrePostes() { return nombrePostes; }
    public void setNombrePostes(int nombrePostes) { this.nombrePostes = nombrePostes; }

    public LocalDate getDateSouhaitee() { return dateSouhaitee; }
    public void setDateSouhaitee(LocalDate dateSouhaitee) { this.dateSouhaitee = dateSouhaitee; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public PrioriteBesoin getPriorite() { return priorite; }
    public void setPriorite(PrioriteBesoin priorite) { this.priorite = priorite; }

    public StatutBesoin getStatut() { return statut; }
    public void setStatut(StatutBesoin statut) { this.statut = statut; }

    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
