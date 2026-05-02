package com.hrflow.candidature.model;

import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Représente un CV déposé pour un projet de recrutement.
 *
 * Contrainte métier : un même email ne peut postuler qu'une seule fois
 * par projet. La contrainte est appliquée APRÈS l'extraction IA (quand
 * emailCandidat est connu). Avant extraction, emailCandidat est NULL —
 * PostgreSQL ne considère pas deux NULLs comme égaux dans une unique
 * constraint, donc plusieurs uploads simultanés ne se bloquent pas.
 */
@Entity
@Table(
    name = "candidatures",
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_candidature_email_projet",
            columnNames = { "email_candidat", "projet_recrutement_id" }
        )
    },
    indexes = {
        @Index(name = "idx_candidature_projet",  columnList = "projet_recrutement_id"),
        @Index(name = "idx_candidature_statut",  columnList = "statut"),
        @Index(name = "idx_candidature_score",   columnList = "score_matching"),
        @Index(name = "idx_candidature_email",   columnList = "email_candidat")
    }
)
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_recrutement_id", nullable = false)
    private ProjetRecrutement projetRecrutement;

    // ── Fichier CV ───────────────────────────────────────────────────────────────

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "chemin_minio", nullable = false, length = 512)
    private String cheminMinio;

    @Column(name = "type_fichier", nullable = false, length = 128)
    private String typeFichier;

    @Column(name = "taille_fichier")
    private Long tailleFichier;

    // ── Données extraites par l'IA (nullable jusqu'à extraction) ─────────────────

    @Column(name = "nom_candidat", length = 200)
    private String nomCandidat;

    /**
     * NULL tant que l'IA n'a pas extrait le CV.
     * Une fois extrait, la contrainte uq_candidature_email_projet
     * empêche deux candidatures avec le même email pour le même projet.
     */
    @Column(name = "email_candidat", length = 255)
    private String emailCandidat;

    @Column(name = "telephone_candidat", length = 50)
    private String telephoneCandidat;

    // ── Évaluation IA ────────────────────────────────────────────────────────────

    @Column(name = "score_matching")
    private Integer scoreMatching;

    @Column(name = "points_forts", columnDefinition = "TEXT")
    private String pointsForts;

    @Column(name = "points_manquants", columnDefinition = "TEXT")
    private String pointsManquants;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommandation", length = 30)
    private RecommandationIA recommandation;

    @Column(name = "justification_ia", columnDefinition = "TEXT")
    private String justificationIa;

    // ── Statut & dates ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutCandidature statut;

    @Column(name = "depose_le", nullable = false, updatable = false)
    private LocalDateTime deposeLe;

    @Column(name = "evalue_le")
    private LocalDateTime evalueLe;

    @Version
    @Column(nullable = false)
    private Integer version;

    @PrePersist
    void prePersist() {
        this.deposeLe = LocalDateTime.now();
        if (this.statut == null) this.statut = StatutCandidature.RECU;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProjetRecrutement getProjetRecrutement() { return projetRecrutement; }
    public void setProjetRecrutement(ProjetRecrutement p) { this.projetRecrutement = p; }

    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String s) { this.nomFichier = s; }

    public String getCheminMinio() { return cheminMinio; }
    public void setCheminMinio(String s) { this.cheminMinio = s; }

    public String getTypeFichier() { return typeFichier; }
    public void setTypeFichier(String s) { this.typeFichier = s; }

    public Long getTailleFichier() { return tailleFichier; }
    public void setTailleFichier(Long l) { this.tailleFichier = l; }

    public String getNomCandidat() { return nomCandidat; }
    public void setNomCandidat(String s) { this.nomCandidat = s; }

    public String getEmailCandidat() { return emailCandidat; }
    public void setEmailCandidat(String s) { this.emailCandidat = s; }

    public String getTelephoneCandidat() { return telephoneCandidat; }
    public void setTelephoneCandidat(String s) { this.telephoneCandidat = s; }

    public Integer getScoreMatching() { return scoreMatching; }
    public void setScoreMatching(Integer i) { this.scoreMatching = i; }

    public String getPointsForts() { return pointsForts; }
    public void setPointsForts(String s) { this.pointsForts = s; }

    public String getPointsManquants() { return pointsManquants; }
    public void setPointsManquants(String s) { this.pointsManquants = s; }

    public RecommandationIA getRecommandation() { return recommandation; }
    public void setRecommandation(RecommandationIA r) { this.recommandation = r; }

    public String getJustificationIa() { return justificationIa; }
    public void setJustificationIa(String s) { this.justificationIa = s; }

    public StatutCandidature getStatut() { return statut; }
    public void setStatut(StatutCandidature s) { this.statut = s; }

    public LocalDateTime getDeposeLe() { return deposeLe; }
    public void setDeposeLe(LocalDateTime d) { this.deposeLe = d; }

    public LocalDateTime getEvalueLe() { return evalueLe; }
    public void setEvalueLe(LocalDateTime d) { this.evalueLe = d; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { this.version = v; }
}
