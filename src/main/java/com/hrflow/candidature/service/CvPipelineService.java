package com.hrflow.candidature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pipeline asynchrone d'évaluation IA d'un CV.
 *
 * Déclenchement : via afterCommit() dans CandidatureService — garantit que la
 * candidature est committée en base avant que ce thread démarre.
 *
 * Pattern deux TX courtes :
 *   TX 1 (courte) : load candidature → EN_COURS → commit → connexion libérée
 *   [Docling (URL présignée), LLM extraction, LLM évaluation — aucune connexion DB tenue]
 *   TX 2 (courte) : reload candidature → persister résultats → EVALUE/ERREUR → commit
 *
 * Architecture URL-source (zéro copie mémoire) :
 *   Ce pipeline ne télécharge jamais le CV. Il génère une URL présignée MinIO
 *   que docling-serve consomme directement. Spring Boot n'est qu'un orchestrateur.
 *   Prérequis : docling-serve doit pouvoir joindre MinIO (hostname interne).
 *
 * Gardes de production :
 *   — Markdown vide : fail-fast si Docling ne peut pas extraire de texte (PDF scanné)
 *   — Cohérence score ↔ recommandation : override si le LLM est incohérent
 *   — markErreur() : la candidature ne reste jamais bloquée en EN_COURS
 *
 * Executor : "pipelineExecutor" (ThreadPoolTaskExecutor borné, voir PipelineAsyncConfig).
 */
@Service
public class CvPipelineService {

    private static final Logger log = LoggerFactory.getLogger(CvPipelineService.class);

    /** Seuil minimum de caractères pour considérer un CV lisible après conversion Docling. */
    private static final int MARKDOWN_MIN_LENGTH = 100;

    private final CandidatureRepository candidatureRepo;
    private final MinioService          minioService;
    private final DoclingService        doclingService;
    private final CvDataExtractor       cvDataExtractor;
    private final CvEvaluator           cvEvaluator;
    private final ObjectMapper          objectMapper;
    private final TransactionTemplate   txWrite;

    public CvPipelineService(CandidatureRepository      candidatureRepo,
                             MinioService               minioService,
                             DoclingService             doclingService,
                             CvDataExtractor            cvDataExtractor,
                             CvEvaluator                cvEvaluator,
                             ObjectMapper               objectMapper,
                             PlatformTransactionManager txManager) {
        this.candidatureRepo = candidatureRepo;
        this.minioService    = minioService;
        this.doclingService  = doclingService;
        this.cvDataExtractor = cvDataExtractor;
        this.cvEvaluator     = cvEvaluator;
        this.objectMapper    = objectMapper;
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

        if (snapshot == null) return;

        // ── Appels externes — aucune connexion DB tenue ───────────────────────
        try {
            // Étape 1 : Génération URL présignée + conversion Docling
            // docling-serve télécharge le CV DIRECTEMENT depuis MinIO via l'URL présignée.
            // Spring Boot ne charge aucun byte en mémoire — zéro copie, zéro Base64.
            log.info("[Pipeline] génération URL présignée MinIO → {}", snapshot.getCheminMinio());
            String documentUrl = minioService.presignedUrl(snapshot.getCheminMinio());
            log.info("[Pipeline] conversion Docling → Markdown ({})", snapshot.getNomFichier());
            String cvMarkdown = doclingService.toMarkdown(documentUrl, snapshot.getNomFichier());

            // Garde : CV vide ou illisible — PDF scanné sans couche texte, fichier corrompu…
            // On fail-fast ici plutôt que de gaspiller deux appels LLM sur du vide.
            if (cvMarkdown == null || cvMarkdown.strip().length() < MARKDOWN_MIN_LENGTH) {
                log.warn("[Pipeline] CV illisible ou trop court ({} chars) pour candidature={}",
                        cvMarkdown == null ? 0 : cvMarkdown.strip().length(), candidatureId);
                markErreur(candidatureId,
                        "Le CV est illisible ou ne contient pas assez de texte extractible. " +
                        "Vérifiez qu'il ne s'agit pas d'un PDF scanné sans couche texte.");
                return;
            }

            // Étape 2 : Extraction IA des coordonnées candidat
            log.info("[Pipeline] extraction LLM (nom/email/tel) pour candidature={}", candidatureId);
            CandidatInfo info = cvDataExtractor.extraire(cvMarkdown);
            log.info("[Pipeline] extrait → nom='{}', email='{}', tel='{}'",
                    info.nomCandidat(), info.emailCandidat(), info.telephoneCandidat());

            // Étape 3 : Contrôle doublon email × projet
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

            // Garde : cohérence score ↔ recommandation
            // Si le LLM retourne score=30 avec "A_CONVOQUER", on corrige depuis le score.
            int score = clampScore(evaluation.scoreMatching());
            RecommandationIA recommandation = enforceCoherence(
                    score, parseRecommandation(evaluation.recommandation()));

            log.info("[Pipeline] score={}, recommandation={}", score, recommandation);

            // TX 2 (courte) : reload entité propre → persister résultats → EVALUE → commit
            final CandidatInfo     finalInfo  = info;
            final EvaluationCv     finalEval  = evaluation;
            final int              finalScore = score;
            final RecommandationIA finalReco  = recommandation;

            txWrite.execute(status -> {
                Candidature c = candidatureRepo.findByIdWithProjet(candidatureId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Candidature disparue en TX2 : " + candidatureId));
                c.setNomCandidat(normalise(finalInfo.nomCandidat()));
                c.setEmailCandidat(normalise(finalInfo.emailCandidat()));
                c.setTelephoneCandidat(normalise(finalInfo.telephoneCandidat()));
                c.setScoreMatching(finalScore);
                c.setPointsForts(toJson(finalEval.pointsForts()));
                c.setPointsManquants(toJson(finalEval.pointsManquants()));
                c.setRecommandation(finalReco);
                c.setJustificationIa(finalEval.justificationIa());
                c.setStatut(StatutCandidature.EVALUE);
                c.setEvalueLe(LocalDateTime.now());
                return candidatureRepo.save(c);
            });

            log.info("[Pipeline] candidature={} → EVALUE — score={}, recommandation={}",
                    candidatureId, finalScore, finalReco);

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
     * La candidature ne reste jamais bloquée en EN_COURS.
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
     * Vérifie la cohérence entre le score et la recommandation retournés par le LLM.
     * Si incohérents, le score est la source de vérité — la recommandation est recalculée.
     * Exemple : score=30 avec "A_CONVOQUER" → corrigé en "NE_CORRESPOND_PAS".
     */
    private static RecommandationIA enforceCoherence(int score, RecommandationIA parsed) {
        RecommandationIA expected = scoreToRecommandation(score);
        if (expected != parsed) {
            log.warn("[Pipeline] incohérence LLM — score={} implique '{}' mais LLM a retourné '{}' → corrigé",
                    score, expected, parsed);
            return expected;
        }
        return parsed;
    }

    /** Calcule la recommandation attendue d'après le barème défini dans le prompt CvEvaluator. */
    private static RecommandationIA scoreToRecommandation(int score) {
        if (score >= 75) return RecommandationIA.A_CONVOQUER;
        if (score >= 45) return RecommandationIA.A_ETUDIER;
        return RecommandationIA.NE_CORRESPOND_PAS;
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

    /**
     * Sérialise une List&lt;String&gt; en JSON pour stockage en colonne TEXT.
     * {@code CandidatureMapper.parseJsonList()} désérialisera ce JSON à la lecture.
     * Retourne "[]" en cas d'erreur ou de liste null/vide — jamais null en base.
     */
    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("[Pipeline] sérialisation JSON échouée : {} — stockage '[]'", e.getMessage());
            return "[]";
        }
    }

    /** Retourne "" si null — évite "null" dans les prompts LLM. */
    private static String safe(String value) {
        return value != null ? value : "";
    }

    /**
     * Convertit la chaîne renvoyée par le LLM en enum RecommandationIA.
     * Tolère variations de casse et séparateurs parasites.
     * Fallback → A_ETUDIER (valeur neutre) — enforceCoherence() corrigera ensuite si besoin.
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
