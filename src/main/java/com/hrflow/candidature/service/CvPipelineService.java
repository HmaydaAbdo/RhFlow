package com.hrflow.candidature.service;

import com.hrflow.ai.dto.CandidatInfo;
import com.hrflow.ai.dto.EvaluationCv;
import com.hrflow.ai.service.CvDataExtractor;
import com.hrflow.ai.service.CvEvaluator;
import com.hrflow.candidature.model.Candidature;
import com.hrflow.candidature.model.RecommandationIA;
import com.hrflow.candidature.model.StatutCandidature;
import com.hrflow.candidature.repository.CandidatureRepository;
import com.hrflow.docling.service.DoclingService;
import com.hrflow.fichedeposte.model.FicheDePoste;
import com.hrflow.storage.service.MinioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * Pipeline asynchrone d'évaluation IA d'un CV.
 *
 * Déclenchement : via afterCommit() dans CandidatureService — garantit que la
 * candidature est committée en base avant que ce thread démarre.
 *
 * Pattern deux TX courtes (même principe que CandidatureService.upload) :
 *
 *   TX 1 (courte) : load candidature → EN_COURS → commit → connexion libérée
 *   [MinIO download, Docling, LLM extraction, LLM évaluation — aucune connexion DB tenue]
 *   TX 2 (courte) : reload candidature → persister résultats → EVALUE/ERREUR → commit
 *
 * Protection globale : tout échec → markErreur() dans une TX propre.
 * La candidature ne reste jamais bloquée en EN_COURS.
 *
 * Executor : "pipelineExecutor" (ThreadPoolTaskExecutor borné, voir PipelineAsyncConfig).
 */
@Service
public class CvPipelineService {

    private static final Logger log = LoggerFactory.getLogger(CvPipelineService.class);

    private final CandidatureRepository candidatureRepo;
    private final MinioService          minioService;
    private final DoclingService        doclingService;
    private final CvDataExtractor       cvDataExtractor;
    private final CvEvaluator           cvEvaluator;
    private final TransactionTemplate   txWrite;

    public CvPipelineService(CandidatureRepository      candidatureRepo,
                             MinioService               minioService,
                             DoclingService             doclingService,
                             CvDataExtractor            cvDataExtractor,
                             CvEvaluator                cvEvaluator,
                             PlatformTransactionManager txManager) {
        this.candidatureRepo = candidatureRepo;
        this.minioService    = minioService;
        this.doclingService  = doclingService;
        this.cvDataExtractor = cvDataExtractor;
        this.cvEvaluator     = cvEvaluator;
        this.txWrite         = new TransactionTemplate(txManager);
    }

    // ── Entrée publique ───────────────────────────────────────────────────────

    @Async("pipelineExecutor")
    public void traiter(Long candidatureId) {
        log.info("[Pipeline] démarrage candidature={}", candidatureId);

        // TX 1 (courte) : load + EN_COURS + commit → connexion libérée immédiatement
        Candidature snapshot = txWrite.execute(status -> {
            var opt = candidatureRepo.findByIdWithProjet(candidatureId);
            if (opt.isEmpty()) {
                log.warn("[Pipeline] candidature={} introuvable — abandon", candidatureId);
                return null;
            }
            Candidature c = opt.get();
            c.setStatut(StatutCandidature.EN_COURS);
            return candidatureRepo.save(c);
        });

        if (snapshot == null) return; // candidature introuvable

        // ── Appels externes — aucune connexion DB tenue ───────────────────────
        try {
            // Étape 1 : Download MinIO + conversion Docling dans un seul try-with-resources
            log.info("[Pipeline] téléchargement MinIO → {}", snapshot.getCheminMinio());
            String cvMarkdown;
            try (InputStream fileStream = minioService.download(snapshot.getCheminMinio())) {
                log.info("[Pipeline] conversion Docling → Markdown ({})", snapshot.getNomFichier());
                cvMarkdown = doclingService.toMarkdown(fileStream, snapshot.getNomFichier());
            }

            // Étape 2 : Extraction IA des coordonnées candidat
            log.info("[Pipeline] extraction LLM (nom/email/tel) pour candidature={}", candidatureId);
            CandidatInfo info = cvDataExtractor.extraire(cvMarkdown);
            log.info("[Pipeline] extrait → nom='{}', email='{}', tel='{}'",
                    info.nomCandidat(), info.emailCandidat(), info.telephoneCandidat());

            // Étape 3 : Contrôle doublon email × projet
            // (le repository crée sa propre TX courte pour cette requête)
            if (info.emailCandidat() != null && !info.emailCandidat().isBlank()) {
                Long projetId = snapshot.getProjetRecrutement().getId();
                boolean doublon = candidatureRepo
                        .existsByEmailCandidatIgnoreCaseAndProjetRecrutementIdAndIdNot(
                                info.emailCandidat(), projetId, candidatureId);
                if (doublon) {
                    log.warn("[Pipeline] doublon détecté — email='{}' pour projet={}",
                            info.emailCandidat(), projetId);
                    markErreur(candidatureId,
                            "Doublon : un CV avec l'adresse email '%s' existe déjà pour ce projet."
                                    .formatted(info.emailCandidat()));
                    return;
                }
            }

            // Étape 4 : Évaluation IA (score + recommandation)
            String fdpTexte = buildFicheDePosteTexte(snapshot.getProjetRecrutement().getFicheDePoste());
            log.info("[Pipeline] évaluation LLM pour candidature={} / poste='{}'",
                    candidatureId, snapshot.getProjetRecrutement().getFicheDePoste().getIntitulePoste());
            EvaluationCv evaluation = cvEvaluator.evaluer(fdpTexte, cvMarkdown);
            log.info("[Pipeline] score={}, recommandation={}",
                    evaluation.scoreMatching(), evaluation.recommandation());

            // TX 2 (courte) : reload entité propre → persister résultats → EVALUE → commit
            final CandidatInfo finalInfo = info;
            final EvaluationCv finalEval = evaluation;
            txWrite.execute(status -> {
                Candidature c = candidatureRepo.findByIdWithProjet(candidatureId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Candidature disparue en TX2 : " + candidatureId));
                c.setNomCandidat(normalise(finalInfo.nomCandidat()));
                c.setEmailCandidat(normalise(finalInfo.emailCandidat()));
                c.setTelephoneCandidat(normalise(finalInfo.telephoneCandidat()));
                c.setScoreMatching(clampScore(finalEval.scoreMatching()));
                c.setPointsForts(finalEval.pointsForts());
                c.setPointsManquants(finalEval.pointsManquants());
                c.setRecommandation(parseRecommandation(finalEval.recommandation()));
                c.setJustificationIa(finalEval.justificationIa());
                c.setStatut(StatutCandidature.EVALUE);
                c.setEvalueLe(LocalDateTime.now());
                return candidatureRepo.save(c);
            });

            log.info("[Pipeline] candidature={} → EVALUE — score={}, recommandation={}",
                    candidatureId, clampScore(evaluation.scoreMatching()),
                    parseRecommandation(evaluation.recommandation()));

        } catch (Exception e) {
            log.error("[Pipeline] échec traitement candidature={} : {}", candidatureId, e.getMessage(), e);
            markErreur(candidatureId, "Erreur système : " + truncate(e.getMessage(), 500));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Recharge la candidature dans une TX propre et la marque ERREUR.
     * Utilise un reload (pas le snapshot détaché de TX 1) pour garantir
     * un état Hibernate sain quelle que soit la cause de l'échec.
     */
    private void markErreur(Long candidatureId, String message) {
        try {
            txWrite.execute(status -> {
                candidatureRepo.findByIdWithProjet(candidatureId).ifPresent(c -> {
                    c.setStatut(StatutCandidature.ERREUR);
                    c.setJustificationIa(message);
                    candidatureRepo.save(c);
                });
                return null;
            });
        } catch (Exception ex) {
            log.error("[Pipeline] impossible de marquer ERREUR pour candidature={} : {}",
                    candidatureId, ex.getMessage());
        }
    }

    /**
     * Construit le contexte textuel de la fiche de poste pour le prompt d'évaluation.
     * safe() remplace null par "" — évite le mot "null" dans le prompt LLM.
     */
    private static String buildFicheDePosteTexte(FicheDePoste fdp) {
        return """
                Poste : %s
                Mission principale : %s
                Activités principales : %s
                Niveau d'études requis : %s
                Domaine de formation : %s
                Années d'expérience requises : %d
                Compétences techniques : %s
                Compétences managériales : %s
                """.formatted(
                safe(fdp.getIntitulePoste()),
                safe(fdp.getMissionPrincipale()),
                safe(fdp.getActivitesPrincipales()),
                safe(fdp.getNiveauEtudes().toString()),
                safe(fdp.getDomaineFormation()),
                fdp.getAnneesExperience(),
                safe(fdp.getCompetencesTechniques()),
                safe(fdp.getCompetencesManageriales())
        );
    }

    /** Retourne "" si null — évite "null" dans les prompts LLM. */
    private static String safe(String value) {
        return value != null ? value : "";
    }

    /**
     * Convertit la chaîne renvoyée par le LLM en enum RecommandationIA.
     * Tolère variations de casse et séparateurs parasites.
     * Fallback → A_ETUDIER (plus neutre que NE_CORRESPOND_PAS en cas de doute).
     */
    private static RecommandationIA parseRecommandation(String raw) {
        if (raw == null) return RecommandationIA.A_ETUDIER;
        String cleaned = raw.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (cleaned) {
            case "A_CONVOQUER"       -> RecommandationIA.A_CONVOQUER;
            case "NE_CORRESPOND_PAS" -> RecommandationIA.NE_CORRESPOND_PAS;
            default                  -> RecommandationIA.A_ETUDIER;
        };
    }

    /** Score borné entre 0 et 100 — protège contre les hallucinations hors-barème. */
    private static int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /** Retourne null si la valeur est null ou vide, sinon la valeur trimmée. */
    private static String normalise(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** Tronque un message d'erreur pour ne pas saturer la colonne justificationIa. */
    private static String truncate(String msg, int max) {
        if (msg == null) return "Erreur inconnue";
        return msg.length() <= max ? msg : msg.substring(0, max) + "…";
    }
}
