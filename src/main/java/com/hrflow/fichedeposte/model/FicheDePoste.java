package com.hrflow.fichedeposte.model;

import com.hrflow.direction.entities.Direction;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fiches_de_poste")
public class FicheDePoste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String intitulePoste;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direction_id", nullable = false)
    private Direction direction;

    @Column(columnDefinition = "TEXT",nullable = false)
    private String missionPrincipale;

    @Column(columnDefinition = "TEXT",nullable = false)
    private String activitesPrincipales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NiveauEtudes niveauEtudes;

    @Column(nullable = false)
    private String domaineFormation;

    @Column(nullable = false)
    private int anneesExperience;

    @Column(columnDefinition = "TEXT",nullable = false)
    private String competencesTechniques;

    @Column(columnDefinition = "TEXT",nullable = false)
    private String competencesManageriales;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;



    public FicheDePoste() {}

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIntitulePoste() { return intitulePoste; }
    public void setIntitulePoste(String intitulePoste) { this.intitulePoste = intitulePoste; }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }

    public String getMissionPrincipale() { return missionPrincipale; }
    public void setMissionPrincipale(String missionPrincipale) { this.missionPrincipale = missionPrincipale; }

    public String getActivitesPrincipales() { return activitesPrincipales; }
    public void setActivitesPrincipales(String activitesPrincipales) { this.activitesPrincipales = activitesPrincipales; }

    public NiveauEtudes getNiveauEtudes() { return niveauEtudes; }
    public void setNiveauEtudes(NiveauEtudes niveauEtudes) { this.niveauEtudes = niveauEtudes; }

    public String getDomaineFormation() { return domaineFormation; }
    public void setDomaineFormation(String domaineFormation) { this.domaineFormation = domaineFormation; }

    public int getAnneesExperience() { return anneesExperience; }
    public void setAnneesExperience(int anneesExperience) { this.anneesExperience = anneesExperience; }

    public String getCompetencesTechniques() { return competencesTechniques; }
    public void setCompetencesTechniques(String competencesTechniques) { this.competencesTechniques = competencesTechniques; }

    public String getCompetencesManageriales() { return competencesManageriales; }
    public void setCompetencesManageriales(String competencesManageriales) { this.competencesManageriales = competencesManageriales; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

}
