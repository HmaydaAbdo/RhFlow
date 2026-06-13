package com.hrflow.ingestion.model;

/**
 * Source d'un {@link IngestionRecord} — d'où vient le CV qui arrive dans la file.
 *
 * <p>L'enum est volontairement extensible : on déclare aujourd'hui {@code EMAIL}
 * (workflow n8n IMAP), mais les autres valeurs sont prévues pour les évolutions
 * futures (SES Inbound webhook, bot Slack, formulaire carrière publique…).
 * Le code applicatif ne route que sur {@code EMAIL} pour l'instant.
 */
public enum IngestionSource {

    /** CV arrivé par email — workflow n8n IMAP → POST /ingest/cv. */
    EMAIL,

    /** CV envoyé via un appel API direct (intégration custom, futur). */
    MANUAL_API,

    /** CV uploadé dans un canal Slack via un bot (futur). */
    SLACK_BOT,

    /** CV soumis via un formulaire public sur le site carrière (futur). */
    WEB_FORM
}
