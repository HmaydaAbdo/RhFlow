package com.hrflow.ingestion.model;

import com.hrflow.candidature.model.Candidature;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Trace d'audit d'un CV arrivant de l'extérieur (mailbox n8n aujourd'hui,
 * futurs canaux ensuite). Existe AVANT la {@link Candidature} et survit à
 * elle — c'est le log unique de tout ce qui rentre, succès comme rejet.
 *
 * <p>Invariants :
 * <ul>
 *   <li>{@code (externalId, source)} est UNIQUE → idempotence forte :
 *       le même email (Message-ID) ne peut être ingéré qu'une seule fois.</li>
 *   <li>{@code receivedAt} immuable — défini par {@link #prePersist()}.</li>
 *   <li>{@code candidature} est NULL tant que {@code status != IMPORTED}.</li>
 *   <li>{@code rejectionReason} est NULL tant que {@code status != REJECTED}.</li>
 * </ul>
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
        @Index(name = "idx_ingestion_status",       columnList = "status"),
        @Index(name = "idx_ingestion_received_at",  columnList = "received_at"),
        @Index(name = "idx_ingestion_candidature",  columnList = "candidature_id"),
        @Index(name = "idx_ingestion_source_status", columnList = "source,status")
    }
)
public class IngestionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identité externe (idempotence) ───────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private IngestionSource source;

    /**
     * Identifiant fourni par la source — pour {@code EMAIL} c'est le header
     * {@code Message-ID} (universellement unique). Combiné avec {@code source}
     * dans une contrainte UNIQUE → garantit qu'on ne traite jamais 2 fois.
     */
    @Column(name = "external_id", nullable = false, length = 512)
    private String externalId;

    // ── Données métier ───────────────────────────────────────────────────────────

    /** Code de référence extrait par regex du sujet, ex. {@code BES-001}. */
    @Column(name = "reference_code", length = 64)
    private String referenceCode;

    /** Nom du fichier de la 1ʳᵉ pièce jointe PDF/DOCX (avant slugify). */
    @Column(name = "nom_fichier", length = 255)
    private String nomFichier;

    /**
     * Méta-données brutes de l'email en JSON (expéditeur, sujet complet, headers
     * pertinents). Conservées pour audit et debugging — pas requêtées.
     */
    @Column(name = "raw_metadata", columnDefinition = "TEXT")
    private String rawMetadata;

    // ── État ─────────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IngestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 50)
    private IngestionRejectionReason rejectionReason;

    /** Texte libre complétant {@code rejectionReason} (ex. code attendu vs reçu). */
    @Column(name = "rejection_detail", columnDefinition = "TEXT")
    private String rejectionDetail;

    /**
     * Candidature créée si {@code status == IMPORTED}. {@code ON DELETE SET NULL}
     * non requis : la candidature ne devrait pas être supprimée tant qu'un record
     * la référence — mais on garde la FK nullable pour permettre l'archivage.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id")
    private Candidature candidature;

    // ── Timestamps ───────────────────────────────────────────────────────────────

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    /** Renseigné à la dernière transition de statut (IMPORTED / REJECTED / ERROR). */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    @PrePersist
    void prePersist() {
        this.receivedAt = LocalDateTime.now();
        if (this.status == null) this.status = IngestionStatus.PENDING;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public IngestionSource getSource() { return source; }
    public void setSource(IngestionSource source) { this.source = source; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }

    public String getRawMetadata() { return rawMetadata; }
    public void setRawMetadata(String rawMetadata) { this.rawMetadata = rawMetadata; }

    public IngestionStatus getStatus() { return status; }
    public void setStatus(IngestionStatus status) { this.status = status; }

    public IngestionRejectionReason getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(IngestionRejectionReason rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getRejectionDetail() { return rejectionDetail; }
    public void setRejectionDetail(String rejectionDetail) { this.rejectionDetail = rejectionDetail; }

    public Candidature getCandidature() { return candidature; }
    public void setCandidature(Candidature candidature) { this.candidature = candidature; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
