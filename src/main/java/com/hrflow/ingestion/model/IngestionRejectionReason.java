package com.hrflow.ingestion.model;

/**
 * Raison métier du rejet d'un {@link IngestionRecord}.
 *
 * <p>Renseigné uniquement quand {@link IngestionStatus#REJECTED}.
 * Stocké en {@code VARCHAR} via {@code @Enumerated(STRING)} — l'évolution
 * de l'enum est compatible (on peut ajouter des valeurs sans casser l'existant).
 *
 * <p>L'UI DRH affiche un libellé fr lisible pour chacune (mapping côté front).
 */
public enum IngestionRejectionReason {

    /** Le sujet de l'email ne contient pas de code de référence (regex {@code [XXX]}). */
    NO_REFERENCE_CODE,

    /** Le code de référence ne correspond à aucun projet de recrutement existant. */
    UNKNOWN_REFERENCE,

    /** Le projet de recrutement matché est fermé — n'accepte plus de candidatures. */
    PROJECT_CLOSED,

    /** Format de fichier non supporté (autre que PDF/DOCX) ou contenu invalide. */
    INVALID_FILE_FORMAT,

    /** Fichier trop volumineux ou dépassant la limite de pages. */
    FILE_TOO_LARGE,

    /** Une candidature avec le même email candidat existe déjà pour ce projet. */
    DUPLICATE_CANDIDATE_EMAIL,

    /** Rejet décidé manuellement par le DRH depuis la « Boîte de réception ». */
    MANUAL_REJECTION
}
