package com.hrflow.ingestion.service;

import com.hrflow.ingestion.dto.IngestionRecordResponse;
import com.hrflow.ingestion.exception.IngestionRecordNotFoundException;
import com.hrflow.ingestion.mapper.IngestionRecordMapper;
import com.hrflow.ingestion.model.IngestionSource;
import com.hrflow.ingestion.model.IngestionStatus;
import com.hrflow.ingestion.repositories.IngestionRecordRepository;
import com.hrflow.ingestion.specifications.IngestionRecordSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service en lecture seule pour la « Boîte de réception » DRH (frontend
 * {@code /inbox}).
 *
 * <p><b>Pourquoi un service séparé de {@link IngestionService} ?</b>
 * <ul>
 *   <li>Séparation read / write — {@code IngestionService} orchestre
 *       l'écriture (création + transitions d'état), {@code IngestionReadService}
 *       sert exclusivement à la consultation.</li>
 *   <li>{@code @Transactional(readOnly = true)} appliqué globalement —
 *       Hibernate peut optimiser (pas de dirty checking, pas de flush).</li>
 *   <li>Surface API restreinte : si demain on ajoute des actions (retry,
 *       delete), elles iront dans un {@code IngestionAdminService}, pas ici.</li>
 * </ul>
 *
 * <p><b>Cap défensif sur la pagination</b> ({@link #MAX_PAGE_SIZE}) : protège
 * contre un client malveillant ou maladroit qui demanderait 10 000 records
 * d'un coup (mémoire JVM + temps de réponse).
 */
@Service
@Transactional(readOnly = true)
public class IngestionReadService {

    /** Limite supérieure imposée à {@code size} — au-delà, on plafonne. */
    private static final int MAX_PAGE_SIZE = 100;

    private final IngestionRecordRepository repository;
    private final IngestionRecordMapper     mapper;

    public IngestionReadService(IngestionRecordRepository repository,
                                IngestionRecordMapper mapper) {
        this.repository = repository;
        this.mapper     = mapper;
    }

    /**
     * Recherche paginée et filtrée des records d'ingestion.
     *
     * <p>Tri par {@code receivedAt} desc — l'UI veut les plus récents en haut
     * (équivalent boîte mail).
     *
     * @param status statut à filtrer (null = tous statuts)
     * @param source source à filtrer (null = toutes sources)
     * @param page   numéro de page 0-indexée (négatif → 0)
     * @param size   taille de page (plafonnée à {@link #MAX_PAGE_SIZE})
     */
    public Page<IngestionRecordResponse> search(IngestionStatus status,
                                                IngestionSource source,
                                                int page,
                                                int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "receivedAt")
        );

        return repository
                .findAll(IngestionRecordSpecification.fromSearch(status, source), pageable)
                .map(mapper::toResponse);
    }

    /**
     * Détail d'un record par id — utilisé par la page {@code /inbox/:id}.
     *
     * @throws IngestionRecordNotFoundException si l'id n'existe pas en base
     *         (mappée vers 404 par {@code IngestionExceptionHandler}).
     */
    public IngestionRecordResponse getById(Long id) {
        return repository.findWithDetailsById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new IngestionRecordNotFoundException(id));
    }
}
