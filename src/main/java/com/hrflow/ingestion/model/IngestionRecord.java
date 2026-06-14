package com.hrflow.ingestion.model;

import com.hrflow.candidature.model.Candidature;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;

import java.time.LocalDateTime;

/**
 * Trace d'audit d'un CV arrivant de l'extérieur (mailbox n8n aujourd'hui,
 * futurs canaux ensuite) OU d'un upload manuel UI. Existe AVANT la
 * {@link Candidature} et survit à elle — c'est le journal d'entrée unique de
 * tout CV qui rentre dans le système, succès comme rejet.
 *
 * <h2>Cycle de vie</h2>
 * <pre>
 *   PENDING ──→ IMPORTED   (markImported + lien candidature)
 *          ╲──→ REJECTED   (reject + raison métier)
 *          ╲──→ ERROR      (markError + détail technique)
 * </pre>
 *
 * <p>Une fois posé en {@code IMPORTED}/{@code REJECTED}/{@code ERROR}, le
 * record est terminal — aucune transition arrière. Toute tentative de
 * transition depuis un état non-PENDING lève une {@link IllegalStateException}.
 *
 * <h2>Encapsulation des invariants</h2>
 *
 * <p>L'entité est <strong>encapsulée</strong> : pas de setters publics pour
 * les champs sensibles (status, rejectionReason, candidature, processedAt).
 * Les transitions passent OBLIGATOIREMENT par :
 * <ul>
 *   <li>{@link #createPending} pour la création (factory)</li>
 *   <li>{@link #markImported(Candidature)} pour le succès</li>
 *   <li>{@link #reject(IngestionRejectionReason, String)} pour les rejets métier</li>
 *   <li>{@link #markError(String)} pour les erreurs techniques</li>
 * </ul>
 *
 * <p>Hibernate accède aux champs directement (field-access, parce que
 * {@code @Id} est sur le champ) — pas besoin de setters pour le mapping ORM.
 *
 * <h2>Invariants protégés au niveau DB (@Check)</h2>
 *
 * <p>Filet de sécurité indépendant du code Java :
 * <ol>
 *   <li>{@code status=IMPORTED ⇒ candidature_id IS NOT NULL}</li>
 *   <li>{@code status=REJECTED ⇒ rejection_reason IS NOT NULL}</li>
 *   <li>{@code status ≠ PENDING ⇒ processed_at IS NOT NULL}</li>
 * </ol>
 *
 * <p>⚠ Avec {@code ddl-auto: update}, les nouvelles contraintes
 * {@code @Check} ne sont PAS appliquées automatiquement sur une table
 * existante. Pour les activer : drop+recreate la table (acceptable en dev)
 * OU exécuter manuellement les {@code ALTER TABLE ADD CONSTRAINT} listés
 * dans le commit message de L52.
 */
@Entity
@Table(
    name = "ingestion_records",
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_ingestion_externalid_source",
            columnNames = { "external_id", "source" }
        )
    },
    indexes = {
        @Index(name = "idx_ingestion_status",        columnList = "status"),
        @Index(name = "idx_ingestion_received_at",   columnList = "received_at"),
        @Index(name = "idx_ingestion_candidature",   columnList = "candidature_id"),
        @Index(name = "idx_ingestion_source_status", columnList = "source,status")
    }
)
// Contraintes DB d'invariant — filet en cas de bug applicatif.
// @Checks (conteneur) pour compatibilité Hibernate 6.x toutes versions.
@Checks({
    @Check(name = "chk_imported_has_candidature",
           constraints = "status <> 'IMPORTED' OR candidature_id IS NOT NULL"),
    @Check(name = "chk_rejected_has_reason",
           constraints = "status <> 'REJECTED' OR rejection_reason IS NOT NULL"),
    @Check(name = "chk_terminal_has_processed_at",
           constraints = "status = 'PENDING' OR processed_at IS NOT NULL")
})
public class IngestionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identité externe (idempotence) ───────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private IngestionSource source;

    @Column(name = "external_id", nullable = false, length = 512)
    private String externalId;

    // ── Données métier ───────────────────────────────────────────────────────────

    @Column(name = "reference_code", length = 64)
    private String referenceCode;

    @Column(name = "nom_fichier", length = 255)
    private String nomFichier;

    @Column(name = "raw_metadata", columnDefinition = "TEXT")
    private String rawMetadata;

    // ── État ─────────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IngestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 50)
    private IngestionRejectionReason rejectionReason;

    @Column(name = "rejection_detail", columnDefinition = "TEXT")
    private String rejectionDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id")
    private Candidature candidature;

    // ── Timestamps ───────────────────────────────────────────────────────────────

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    // ── Construction ─────────────────────────────────────────────────────────────

    /** Required by JPA. Use {@link #createPending} for new instances. */
    protected IngestionRecord() {}

    /**
     * Crée un record dans l'état initial {@link IngestionStatus#PENDING}.
     * {@code receivedAt} sera renseigné par {@link #prePersist()} au flush.
     */
    public static IngestionRecord createPending(IngestionSource source,
                                                String          externalId,
                                                String          referenceCode,
                                                String          nomFichier,
                                                String          rawMetadata) {
        if (source == null) {
            throw new IllegalArgumentException("source est obligatoire");
        }
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId est obligatoire");
        }
        IngestionRecord r = new IngestionRecord();
        r.source        = source;
        r.externalId    = externalId;
        r.referenceCode = referenceCode;
        r.nomFichier    = nomFichier;
        r.rawMetadata   = rawMetadata;
        r.status        = IngestionStatus.PENDING;
        return r;
    }

    @PrePersist
    void prePersist() {
        if (this.receivedAt == null) this.receivedAt = LocalDateTime.now();
        if (this.status     == null) this.status     = IngestionStatus.PENDING;
    }

    // ── Transitions d'état (domaine) ─────────────────────────────────────────────

    /**
     * Marque le record comme importé avec succès et lie la candidature créée.
     *
     * @throws IllegalStateException si {@code status != PENDING}
     * @throws IllegalArgumentException si {@code candidature == null}
     */
    public void markImported(Candidature candidature) {
        requirePending();
        if (candidature == null) {
            throw new IllegalArgumentException(
                    "candidature requise pour markImported (record id=" + id + ")");
        }
        this.candidature   = candidature;
        this.status        = IngestionStatus.IMPORTED;
        this.processedAt   = LocalDateTime.now();
    }

    /**
     * Marque le record comme rejeté pour cause métier (référence inconnue,
     * projet fermé, fichier invalide, doublon email…).
     *
     * @throws IllegalStateException si {@code status != PENDING}
     * @throws IllegalArgumentException si {@code reason == null}
     */
    public void reject(IngestionRejectionReason reason, String detail) {
        requirePending();
        if (reason == null) {
            throw new IllegalArgumentException(
                    "reason requise pour reject (record id=" + id + ")");
        }
        this.rejectionReason = reason;
        this.rejectionDetail = detail;
        this.status          = IngestionStatus.REJECTED;
        this.processedAt     = LocalDateTime.now();
    }

    /**
     * Marque le record en erreur technique (MinIO down, etc.) — peut être
     * retraité plus tard. {@code rejectionDetail} sert ici à stocker le message
     * d'erreur (réutilisation cohérente du champ).
     *
     * @throws IllegalStateException si {@code status != PENDING}
     */
    public void markError(String detail) {
        requirePending();
        this.rejectionDetail = detail;
        this.status          = IngestionStatus.ERROR;
        this.processedAt     = LocalDateTime.now();
    }

    private void requirePending() {
        if (this.status != IngestionStatus.PENDING) {
            throw new IllegalStateException(
                    "Transition impossible — record id=%d est en état %s, attendu PENDING."
                            .formatted(id, status));
        }
    }

    // ── Getters (read-only) ──────────────────────────────────────────────────────

    public Long                     getId()              { return id; }
    public IngestionSource          getSource()          { return source; }
    public String                   getExternalId()      { return externalId; }
    public String                   getReferenceCode()   { return referenceCode; }
    public String                   getNomFichier()      { return nomFichier; }
    public String                   getRawMetadata()     { return rawMetadata; }
    public IngestionStatus          getStatus()          { return status; }
    public IngestionRejectionReason getRejectionReason() { return rejectionReason; }
    public String                   getRejectionDetail() { return rejectionDetail; }
    public Candidature              getCandidature()     { return candidature; }
    public LocalDateTime            getReceivedAt()      { return receivedAt; }
    public LocalDateTime            getProcessedAt()     { return processedAt; }
    public Integer                  getVersion()         { return version; }
}
