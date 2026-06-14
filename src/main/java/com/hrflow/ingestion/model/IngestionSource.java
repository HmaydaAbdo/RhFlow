package com.hrflow.ingestion.model;

/**
 * Source d'un {@link IngestionRecord} — d'où vient le CV qui arrive dans la file.
 *
 * <p>Toute candidature (manuelle ou automatisée) laisse une trace IngestionRecord
 * → journal d'entrée unique et exhaustif.
 *
 * <p>L'enum est extensible : seules {@code MANUAL_UI} et {@code EMAIL} sont
 * actives aujourd'hui, les autres valeurs sont préparées pour les évolutions
 * futures (intégration API tierce, bot Slack, formulaire carrière public…).
 */
public enum IngestionSource {

    /** CV uploadé manuellement par un utilisateur via l'interface RH Flow. */
    MANUAL_UI,

    /** CV arrivé par email — workflow n8n IMAP → POST /ingest/cv. */
    EMAIL,

    /** CV envoyé via un appel API direct (intégration custom, futur). */
    MANUAL_API,

    /** CV uploadé dans un canal Slack via un bot (futur). */
    SLACK_BOT,

    /** CV soumis via un formulaire public sur le site carrière (futur). */
    WEB_FORM
}
