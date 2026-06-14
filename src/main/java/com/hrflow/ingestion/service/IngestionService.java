package com.hrflow.ingestion.service;

import com.hrflow.candidature.model.Candidature;
import com.hrflow.candidature.service.CandidatureService;
import com.hrflow.ingestion.dto.IngestionRecordResponse;
import com.hrflow.ingestion.mapper.IngestionRecordMapper;
import com.hrflow.ingestion.model.IngestionRecord;
import com.hrflow.ingestion.model.IngestionRejectionReason;
import com.hrflow.ingestion.model.IngestionSource;
import com.hrflow.ingestion.model.IngestionStatus;
import com.hrflow.ingestion.repositories.IngestionRecordRepository;
import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import com.hrflow.projetrecrutement.model.StatutProjet;
import com.hrflow.projetrecrutement.repositories.ProjetRecrutementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orchestre l'ingestion d'un CV venant d'une source externe (workflow n8n IMAP
 * aujourd'hui, futurs canaux ensuite).
 *
 * <p>Garanties fortes apportées par ce service :
 * <ul>
 *   <li><b>Idempotence</b> : la contrainte UNIQUE {@code (external_id, source)}
 *       sur {@code ingestion_records} fait que le même Message-ID ne peut être
 *       traité qu'une seule fois — même en cas de retry n8n ou de double poll.</li>
 *   <li><b>Audit complet</b> : chaque appel laisse un {@code IngestionRecord} en
 *       base, peu importe le verdict (importé, rejeté, erreur). Le DRH voit tout
 *       dans la « Boîte de réception ».</li>
 *   <li><b>Rejets métier ≠ erreurs techniques</b> : les rejets (code inconnu,
 *       projet fermé, format invalide) renvoient un 200 OK avec status=REJECTED
 *       dans le body — n8n ne retry pas. Les erreurs techniques (MinIO down…)
 *       marquent le record ERROR ; n8n peut retry plus tard.</li>
 * </ul>
 *
 * <p>Pattern transactionnel : multi-TX courtes (cf. discussion architecture).
 * Aucun {@code @Transactional} au niveau classe — chaque écriture DB passe par
 * {@code txWrite.execute(...)} pour garder le commit immédiat et éviter de tenir
 * une connexion pendant les appels externes (MinIO, pipeline IA).
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final IngestionRecordRepository    repo;
    private final ProjetRecrutementRepository  projetRepo;
    private final CandidatureService           candidatureService;
    private final IngestionRecordMapper        mapper;
    private final TransactionTemplate          txWrite;

    public IngestionService(IngestionRecordRepository    repo,
                            ProjetRecrutementRepository  projetRepo,
                            CandidatureService           candidatureService,
                            IngestionRecordMapper        mapper,
                            PlatformTransactionManager   txManager) {
        this.repo               = repo;
        this.projetRepo         = projetRepo;
        this.candidatureService = candidatureService;
        this.mapper             = mapper;
        this.txWrite            = new TransactionTemplate(txManager);
    }

    // ── API publique ─────────────────────────────────────────────────────────────

    /**
     * Point d'entrée unique pour ingérer un CV depuis une source externe.
     *
     * @param file          fichier CV (PDF/DOCX attendu)
     * @param source        canal d'origine (EMAIL pour n8n IMAP)
     * @param externalId    identifiant unique côté source (Message-ID pour email)
     * @param referenceCode code projet extrait du sujet (peut être null)
     * @param rawMetadata   JSON brut pour audit (expéditeur, sujet…) — peut être null
     * @return l'état du record après traitement (verdict métier ou erreur)
     */
    public IngestionRecordResponse ingest(MultipartFile  file,
                                          IngestionSource source,
                                          String          externalId,
                                          String          referenceCode,
                                          String          rawMetadata) {

        log.info("[Ingestion] reçu : source={}, externalId={}, ref={}, fichier={}",
                source, externalId, referenceCode,
                file != null ? file.getOriginalFilename() : null);

        // ── Phase 1 : Idempotence — déjà vu ? ──────────────────────────────────
        Optional<IngestionRecord> existing = repo.findByExternalIdAndSource(externalId, source);
        if (existing.isPresent()) {
            log.info("[Ingestion] externalId={} déjà traité (status={}) — skip",
                    externalId, existing.get().getStatus());
            return mapper.toResponse(existing.get());
        }

        // ── Phase 2 : Créer le record PENDING (commit immédiat) ───────────────
        IngestionRecord record;
        try {
            record = txWrite.execute(status -> {
                IngestionRecord r = new IngestionRecord();
                r.setSource(source);
                r.setExternalId(externalId);
                r.setReferenceCode(blankToNull(referenceCode));
                r.setRawMetadata(rawMetadata);
                r.setNomFichier(file != null ? file.getOriginalFilename() : null);
                r.setStatus(IngestionStatus.PENDING);
                return repo.saveAndFlush(r);
            });
        } catch (DataIntegrityViolationException e) {
            // Race condition : un autre thread (autre worker n8n) a inséré entre nos 2 phases.
            // On re-fetch et on retourne son état — c'est exactement le comportement attendu.
            log.warn("[Ingestion] race condition sur externalId={} — fetch existant", externalId);
            return mapper.toResponse(
                    repo.findByExternalIdAndSource(externalId, source)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Race condition non résolue pour externalId=" + externalId))
            );
        }

        // ── Phase 3 : Routing métier (rejets) ──────────────────────────────────
        // (a) Pas de code de référence dans le sujet
        if (record.getReferenceCode() == null) {
            return reject(record, IngestionRejectionReason.NO_REFERENCE_CODE,
                    "Le sujet de l'email ne contient pas de code projet (format attendu : [BES-XXX]).");
        }

        // (b) Code de référence inconnu côté base
        Optional<ProjetRecrutement> projetOpt =
                projetRepo.findByObjetCandidatureIgnoreCase(record.getReferenceCode());
        if (projetOpt.isEmpty()) {
            return reject(record, IngestionRejectionReason.UNKNOWN_REFERENCE,
                    "Aucun projet de recrutement ne correspond au code « %s »."
                            .formatted(record.getReferenceCode()));
        }
        ProjetRecrutement projet = projetOpt.get();

        // (c) Projet trouvé mais fermé
        if (projet.getStatut() == StatutProjet.FERME) {
            return reject(record, IngestionRejectionReason.PROJECT_CLOSED,
                    "Le projet « %s » est fermé et n'accepte plus de candidatures."
                            .formatted(record.getReferenceCode()));
        }

        // (d) Fichier vide → REJECTED INVALID_FILE_FORMAT
        if (file == null || file.isEmpty()) {
            return reject(record, IngestionRejectionReason.INVALID_FILE_FORMAT,
                    "Aucune pièce jointe valide n'a été reçue.");
        }
        // (e) Soft cap métier (taille en octets)
        try {
            candidatureService.validateFileSize(file);
        } catch (IllegalArgumentException e) {
            return reject(record, IngestionRejectionReason.FILE_TOO_LARGE, e.getMessage());
        }
        // (f) Limite nombre de pages (PDF/DOCX trop touffus)
        try {
            candidatureService.validatePageCount(file);
        } catch (IllegalArgumentException e) {
            return reject(record, IngestionRejectionReason.FILE_TOO_LARGE, e.getMessage());
        }

        // ── Phase 4 : Délégation à CandidatureService.createCandidature ────────
        // Lui-même : TX persistance + MinIO + pipeline async (cf. CandidatureService L46).
        Candidature candidature;
        try {
            candidature = candidatureService.createCandidature(projet, file);
        } catch (DataIntegrityViolationException e) {
            // Contrainte uq_candidature_email_projet : doublon email candidat × projet.
            // Catch ici parce qu'on peut le traduire en rejet métier, pas en erreur tech.
            return reject(record, IngestionRejectionReason.DUPLICATE_CANDIDATE_EMAIL,
                    "Une candidature avec le même email candidat existe déjà pour ce projet.");
        } catch (RuntimeException e) {
            // Erreur technique (MinIO indisponible, DB injoignable, etc.) — ERROR.
            // n8n peut retry plus tard, ou le DRH peut rejouer depuis l'UI.
            log.error("[Ingestion] erreur technique sur record={} : {}",
                    record.getId(), e.getMessage(), e);
            return markError(record, "Erreur technique : " + truncate(e.getMessage(), 500));
        }

        // ── Phase 5 : Finaliser — IMPORTED + lien candidature ──────────────────
        final Candidature createdCandidature = candidature;  // effectively final pour le lambda
        IngestionRecord saved = txWrite.execute(status -> {
            // Re-fetch dans la TX pour éviter les LazyInit + state stale
            IngestionRecord r = repo.findById(record.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "IngestionRecord disparu en phase 5 : id=" + record.getId()));
            r.setCandidature(createdCandidature);
            r.setStatus(IngestionStatus.IMPORTED);
            r.setProcessedAt(LocalDateTime.now());
            return repo.save(r);
        });

        log.info("[Ingestion] IMPORTED record={} → candidature={}",
                saved.getId(), candidature.getId());
        return mapper.toResponse(saved);
    }

    // ── Helpers privés — transitions d'état ──────────────────────────────────────

    /**
     * Marque le record REJECTED avec une raison métier, persiste, et retourne le DTO.
     * Le rejet n'est PAS une exception — c'est un état métier valide qui produit un 200.
     */
    private IngestionRecordResponse reject(IngestionRecord            record,
                                           IngestionRejectionReason   reason,
                                           String                     detail) {
        log.info("[Ingestion] REJECTED record={} → reason={} ({})",
                record.getId(), reason, detail);

        IngestionRecord saved = txWrite.execute(status -> {
            IngestionRecord r = repo.findById(record.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "IngestionRecord disparu lors du rejet : id=" + record.getId()));
            r.setStatus(IngestionStatus.REJECTED);
            r.setRejectionReason(reason);
            r.setRejectionDetail(detail);
            r.setProcessedAt(LocalDateTime.now());
            return repo.save(r);
        });
        return mapper.toResponse(saved);
    }

    /**
     * Marque le record ERROR pour les échecs techniques retry-ables.
     * Le détail va dans {@code rejectionDetail} (même colonne, différente sémantique).
     */
    private IngestionRecordResponse markError(IngestionRecord record, String detail) {
        IngestionRecord saved = txWrite.execute(status -> {
            IngestionRecord r = repo.findById(record.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "IngestionRecord disparu lors du markError : id=" + record.getId()));
            r.setStatus(IngestionStatus.ERROR);
            r.setRejectionDetail(detail);
            r.setProcessedAt(LocalDateTime.now());
            return repo.save(r);
        });
        return mapper.toResponse(saved);
    }

    // ── Utility ──────────────────────────────────────────────────────────────────

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String truncate(String msg, int max) {
        if (msg == null) return "(sans message)";
        return msg.length() <= max ? msg : msg.substring(0, max) + "…";
    }
}
