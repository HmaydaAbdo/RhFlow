package com.hrflow.ingestion.service;

import com.hrflow.candidature.model.Candidature;
import com.hrflow.ingestion.event.IngestionErroredEvent;
import com.hrflow.ingestion.event.IngestionImportedEvent;
import com.hrflow.ingestion.event.IngestionRejectedEvent;
import com.hrflow.ingestion.model.IngestionRecord;
import com.hrflow.ingestion.model.IngestionRejectionReason;
import com.hrflow.ingestion.model.IngestionSource;
import com.hrflow.ingestion.repositories.IngestionRecordRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

/**
 * Service utilitaire qui encapsule le <strong>cycle de vie d'un
 * {@link IngestionRecord}</strong> : création PENDING + transitions
 * (IMPORTED / REJECTED / ERROR) + lookup d'idempotence.
 *
 * <p>Bénéfices :
 * <ul>
 *   <li><b>SRP</b> : ni {@code IngestionService} ni {@code CandidatureService}
 *       ne connaissent {@code IngestionRecordRepository} ni {@code txWrite}.
 *       Ils délèguent les transitions à ce recorder.</li>
 *   <li><b>Découplage</b> : {@code CandidatureService} ne dépend plus du
 *       repository ingestion — seulement de cette abstraction de haut niveau.</li>
 *   <li><b>DRY</b> : pattern « TX courte, re-fetch, transition, save » écrit
 *       une seule fois ici, réutilisé par les deux flux (UI manuel, n8n).</li>
 *   <li><b>Testabilité</b> : facile à mocker dans les tests des services métier.</li>
 * </ul>
 *
 * <p>Pattern transactionnel : chaque méthode ouvre sa propre TX courte via
 * {@link TransactionTemplate}, commit immédiatement, libère la connexion. Aucun
 * {@code @Transactional} au niveau classe — la frontière est programmatique.
 *
 * <p>Sémantique d'erreur : aucune méthode ne « swallow » silencieusement. Si une
 * transition échoue (DB injoignable, etc.), l'exception remonte. Le caller
 * décide de wrap-and-log (cas du flux upload manuel) ou de laisser le handler
 * global retourner 500 (cas du flux ingest n8n).
 */
@Service
public class IngestionRecorder {

    private final IngestionRecordRepository repo;
    private final ApplicationEventPublisher publisher;
    private final TransactionTemplate       txWrite;

    public IngestionRecorder(IngestionRecordRepository  repo,
                             ApplicationEventPublisher  publisher,
                             PlatformTransactionManager txManager) {
        this.repo      = repo;
        this.publisher = publisher;
        this.txWrite   = new TransactionTemplate(txManager);
    }

    // ── Lookup d'idempotence ─────────────────────────────────────────────────────

    /**
     * Retourne le record déjà existant pour ce {@code (externalId, source)} si
     * présent — la base de l'idempotence forte garantie par la contrainte
     * UNIQUE de l'entité.
     */
    public Optional<IngestionRecord> findIdempotent(String externalId, IngestionSource source) {
        return repo.findByExternalIdAndSource(externalId, source);
    }

    // ── Création ─────────────────────────────────────────────────────────────────

    /**
     * Crée un record dans l'état {@code PENDING} via la factory de l'entité,
     * puis flush dans une TX courte (commit immédiat) — ce qui rend la ligne
     * visible aux autres connexions et active la garde d'unicité côté DB.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException si un autre
     *         appel simultané a déjà inséré le même {@code (externalId, source)}
     *         (race condition). Le caller doit catch et re-fetch.
     */
    public IngestionRecord createPending(IngestionSource source,
                                         String          externalId,
                                         String          referenceCode,
                                         String          nomFichier,
                                         String          rawMetadata) {
        return txWrite.execute(status -> {
            IngestionRecord r = IngestionRecord.createPending(
                    source, externalId, referenceCode, nomFichier, rawMetadata);
            return repo.saveAndFlush(r);
        });
    }

    // ── Transitions ──────────────────────────────────────────────────────────────

    /**
     * Marque le record IMPORTED et lie la candidature créée. TX courte commit.
     * Re-fetch dans la TX pour repartir d'un état Hibernate propre (le caller
     * peut détenir une référence détachée).
     */
    public IngestionRecord markImported(Long recordId, Candidature candidature) {
        IngestionRecord saved = txWrite.execute(status -> {
            IngestionRecord r = mustFind(recordId);
            r.markImported(candidature);  // domain method — valide PENDING + non-null
            return repo.save(r);
        });
        // Event publié APRÈS commit : tout listener voit un état persisté cohérent.
        publisher.publishEvent(new IngestionImportedEvent(
                saved.getId(), saved.getSource(), saved.getExternalId(),
                candidature.getId(), saved.getProcessedAt()));
        return saved;
    }

    /**
     * Marque le record REJECTED avec une raison métier typée + détail libre.
     */
    public IngestionRecord markRejected(Long                     recordId,
                                        IngestionRejectionReason reason,
                                        String                   detail) {
        IngestionRecord saved = txWrite.execute(status -> {
            IngestionRecord r = mustFind(recordId);
            r.reject(reason, detail);  // domain method — valide PENDING + non-null reason
            return repo.save(r);
        });
        publisher.publishEvent(new IngestionRejectedEvent(
                saved.getId(), saved.getSource(), saved.getExternalId(),
                reason, detail, saved.getProcessedAt()));
        return saved;
    }

    /**
     * Marque le record ERROR avec le détail technique du problème (retry possible).
     */
    public IngestionRecord markError(Long recordId, String detail) {
        IngestionRecord saved = txWrite.execute(status -> {
            IngestionRecord r = mustFind(recordId);
            r.markError(detail);  // domain method — valide PENDING
            return repo.save(r);
        });
        publisher.publishEvent(new IngestionErroredEvent(
                saved.getId(), saved.getSource(), saved.getExternalId(),
                detail, saved.getProcessedAt()));
        return saved;
    }

    // ── Helper ───────────────────────────────────────────────────────────────────

    private IngestionRecord mustFind(Long recordId) {
        return repo.findById(recordId)
                .orElseThrow(() -> new IllegalStateException(
                        "IngestionRecord disparu en cours de traitement : id=" + recordId));
    }
}
