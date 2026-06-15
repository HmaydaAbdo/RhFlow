package com.hrflow.ingestion.event;

import com.hrflow.ingestion.model.IngestionSource;

import java.time.LocalDateTime;

/**
 * Type de base de tous les événements de domaine publiés par
 * {@code IngestionRecorder} lors d'une transition de cycle de vie.
 *
 * <p><b>Sealed interface</b> : un listener peut s'abonner soit au type général
 * {@code IngestionEvent} (pour réagir à toutes les transitions), soit à une
 * sous-classe spécifique ({@link IngestionImportedEvent},
 * {@link IngestionRejectedEvent}, {@link IngestionErroredEvent}).
 *
 * <p>Spring infère automatiquement le type d'événement à partir du paramètre
 * du listener — pas de configuration nécessaire :
 *
 * <pre>{@code
 * @EventListener
 * public void onImported(IngestionImportedEvent e) { ... }
 *
 * @EventListener
 * public void onAnyTransition(IngestionEvent e) { ... }
 * }</pre>
 *
 * <p><b>Sémantique de timing</b> : les events sont publiés <strong>après</strong>
 * le commit de la TX qui a changé l'état du record — le listener voit donc
 * toujours un état cohérent en base. Si un listener doit s'inscrire dans la
 * même TX (par exemple pour annuler la transition en cas d'erreur), il faut
 * utiliser {@code @TransactionalEventListener} avec une phase explicite ; ce
 * n'est pas notre besoin aujourd'hui.
 */
public sealed interface IngestionEvent
        permits IngestionImportedEvent,
                IngestionRejectedEvent,
                IngestionErroredEvent {

    /** Id du record concerné — utile pour aller chercher des infos complémentaires. */
    Long recordId();

    /** Source du record (EMAIL, MANUAL_UI…) — utile pour filtrer côté listener. */
    IngestionSource source();

    /** Identifiant externe (Message-ID email ou "manual-{UUID}"). */
    String externalId();

    /** Timestamp de la transition vers l'état terminal. */
    LocalDateTime processedAt();
}
