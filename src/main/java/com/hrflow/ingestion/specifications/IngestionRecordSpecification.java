package com.hrflow.ingestion.specifications;

import com.hrflow.ingestion.model.IngestionRecord;
import com.hrflow.ingestion.model.IngestionSource;
import com.hrflow.ingestion.model.IngestionStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spécifications JPA pour la « Boîte de réception » DRH.
 *
 * <p>Chaque filtre est optionnel : si le critère est {@code null}, la
 * spécification renvoie {@code cb.conjunction()} (no-op qui ne filtre rien).
 * Cela permet de combiner les critères librement avec {@link Specification#allOf}.
 *
 * <p>Pattern identique à {@code CandidatureSpecification}, pour cohérence
 * cross-module.
 */
public final class IngestionRecordSpecification {

    private IngestionRecordSpecification() {}

    // ── Factory depuis filtres ────────────────────────────────────────────────

    /**
     * Combine tous les filtres optionnels en une seule {@link Specification}.
     * Si TOUS les critères sont null, équivaut à un {@code findAll} sans filtre.
     */
    public static Specification<IngestionRecord> fromSearch(IngestionStatus status,
                                                            IngestionSource source) {
        return Specification.allOf(
                byStatus(status),
                bySource(source)
        );
    }

    // ── Filtres individuels ───────────────────────────────────────────────────

    /** Filtre optionnel sur le statut du cycle de vie (PENDING/IMPORTED/REJECTED/ERROR). */
    public static Specification<IngestionRecord> byStatus(IngestionStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    /** Filtre optionnel sur le canal d'origine (EMAIL pour n8n IMAP, MANUAL_UI pour upload). */
    public static Specification<IngestionRecord> bySource(IngestionSource source) {
        return (root, query, cb) -> {
            if (source == null) return cb.conjunction();
            return cb.equal(root.get("source"), source);
        };
    }
}
