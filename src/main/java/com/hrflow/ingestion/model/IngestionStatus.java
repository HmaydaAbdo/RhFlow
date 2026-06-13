package com.hrflow.ingestion.model;

/**
 * État d'un {@link IngestionRecord} dans son cycle de vie.
 *
 * <p>Cycle de vie normal :
 * <pre>
 *   PENDING ──→ IMPORTED   (CV créé en base, candidature_id renseigné)
 *           ╲
 *            ──→ REJECTED  (raison métier — voir IngestionRejectionReason)
 *           ╲
 *            ──→ ERROR     (erreur technique — retry possible plus tard)
 * </pre>
 *
 * <p>Un record en {@code REJECTED} peut être rattrapé manuellement par le DRH
 * depuis la « Boîte de réception » : il choisit un projet OUVERT et le record
 * passe en {@code IMPORTED} après création de la candidature correspondante.
 */
public enum IngestionStatus {

    /** État initial — record créé, traitement en cours ou pas encore démarré. */
    PENDING,

    /** Succès : candidature créée, FK candidature_id renseigné. */
    IMPORTED,

    /** Rejet métier : raison détaillée dans {@code rejectionReason}. */
    REJECTED,

    /** Erreur technique (5xx interne, MinIO down, etc.) — peut être rejoué. */
    ERROR
}
