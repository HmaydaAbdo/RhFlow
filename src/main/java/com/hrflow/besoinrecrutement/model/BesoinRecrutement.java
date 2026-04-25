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
        @Index(name = "idx_besoin_fiche_encours",  columnList = "fiche_de_poste_id, encours"),
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

    /** Directeur de la direction concernée (déduit de la fiche de poste). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "directeur_id", nullable = false)
    private User directeur;

    /** Utilisateur authentifié qui a exprimé le besoin (peut être DRH, ADMIN ou DIRECTEUR). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private int nombrePostes;

    @Column(nullable = false)
    private LocalDate dateSouhaitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioriteBesoin priorite;

    /**
     * Indique si le besoin est encore en attente de décision.
     * true  = aucune décision prise (besoin actif/récent)
     * false = décision prise (ACCEPTE ou REFUSE)
     */
    @Column(nullable = false)
    private boolean encours;

    /**
     * Décision DRH. Null tant que encours=true.
     * Valorisé à ACCEPTE ou REFUSE lors de la décision.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private StatutBesoin statut;

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
        // encours et statut sont positionnés explicitement par le service / seeder
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

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public int getNombrePostes() { return nombrePostes; }
    public void setNombrePostes(int nombrePostes) { this.nombrePostes = nombrePostes; }

    public LocalDate getDateSouhaitee() { return dateSouhaitee; }
    public void setDateSouhaitee(LocalDate dateSouhaitee) { this.dateSouhaitee = dateSouhaitee; }

    public PrioriteBesoin getPriorite() { return priorite; }
    public void setPriorite(PrioriteBesoin priorite) { this.priorite = priorite; }

    public boolean isEncours() { return encours; }
    public void setEncours(boolean encours) { this.encours = encours; }

    public StatutBesoin getStatut() { return statut; }
    public void setStatut(StatutBesoin statut) { this.statut = statut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
