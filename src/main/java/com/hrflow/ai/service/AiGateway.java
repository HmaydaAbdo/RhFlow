package com.hrflow.ai.service;

import com.hrflow.ai.dto.CandidatInfo;
import com.hrflow.ai.dto.EvaluationCv;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Façade résiliente sur les services IA LangChain4j.
 *
 * Problème résolu : les beans {@code @AiService} de LangChain4j sont des proxys JDK
 * (interfaces). Appliquer {@code @Retryable} ou {@code @CircuitBreaker} directement
 * sur une interface crée un double-proxying fragile. Cette façade concrète encapsule
 * les deux interfaces et expose des méthodes sur lesquelles Spring AOP crée un proxy
 * CGLIB stable.
 *
 * Couche résilience (ordre d'exécution AOP) :
 *
 *   [Retry (outer)] → [CircuitBreaker (inner)] → méthode LangChain4j
 *
 *   1. Spring Retry intercepte l'appel en premier.
 *   2. Resilience4j CB s'exécute à l'intérieur du retry.
 *   3. Si le CB est OUVERT → {@code CallNotPermittedException} levée immédiatement.
 *      {@code noRetryFor = CallNotPermittedException.class} stoppe le retry.
 *   4. Si le CB est FERMÉ → tentative normale, retry sur RuntimeException transitoire.
 *
 * Politique Retry :
 *  — maxAttempts = 3 (1 tentative initiale + 2 retries)
 *  — backoff exponentiel : 1 s → 2 s (couvre HTTP 429 et timeouts transitoires)
 *  — retryFor = RuntimeException (LangChain4j encapsule toutes erreurs réseau/API)
 *  — noRetryFor = {IllegalArgumentException, CallNotPermittedException}
 *
 * Politique Circuit Breaker :
 *  — aiExtract : slidingWindow=10, failureRate=60 %, waitOpen=30 s
 *  — aiEvaluate : même config (deux CB distincts pour monitoring indépendant)
 *  — Métriques CB exposées dans /actuator/health et /actuator/metrics
 *
 * En cas d'échec définitif (retries épuisés ou CB ouvert), l'exception remonte
 * au pipeline qui marque la candidature ERREUR via {@code markErreur()}.
 */
@Service
public class AiGateway {

    private static final Logger log = LoggerFactory.getLogger(AiGateway.class);

    private final CvDataExtractor extractor;
    private final CvEvaluator     evaluator;

    public AiGateway(CvDataExtractor extractor, CvEvaluator evaluator) {
        this.extractor = extractor;
        this.evaluator = evaluator;
    }

    /**
     * Extraction des coordonnées candidat depuis le Markdown du CV.
     *
     * Circuit Breaker "aiExtract" : ouvre après 60 % d'échecs sur 10 appels.
     * Retry × 3 avec backoff exponentiel 1 s → 2 s.
     *
     * @param cvMarkdown texte Markdown du CV (issu de Docling)
     * @return coordonnées extraites (champs null si absents du CV)
     * @throws RuntimeException              si le provider LLM échoue après 3 tentatives
     * @throws CallNotPermittedException     si le circuit breaker est ouvert
     */
    @CircuitBreaker(name = "aiExtract")
    @Retryable(
        retryFor    = RuntimeException.class,
        noRetryFor  = {IllegalArgumentException.class, CallNotPermittedException.class},
        maxAttempts = 3,
        backoff     = @Backoff(delay = 1000, multiplier = 2, maxDelay = 8000)
    )
    public CandidatInfo extraire(String cvMarkdown) {
        log.debug("[AiGateway] extraction coordonnées candidat");
        return extractor.extraire(cvMarkdown);
    }

    /**
     * Évaluation IA de la correspondance CV ↔ fiche de poste.
     *
     * Circuit Breaker "aiEvaluate" : ouvre après 60 % d'échecs sur 10 appels.
     * Retry × 3 avec backoff exponentiel 1 s → 2 s.
     *
     * @param ficheDePoste description textuelle de la fiche de poste
     * @param cvMarkdown   texte Markdown du CV
     * @return évaluation (score, points forts/manquants, recommandation, justification)
     * @throws RuntimeException              si le provider LLM échoue après 3 tentatives
     * @throws CallNotPermittedException     si le circuit breaker est ouvert
     */
    @CircuitBreaker(name = "aiEvaluate")
    @Retryable(
        retryFor    = RuntimeException.class,
        noRetryFor  = {IllegalArgumentException.class, CallNotPermittedException.class},
        maxAttempts = 3,
        backoff     = @Backoff(delay = 1000, multiplier = 2, maxDelay = 8000)
    )
    public EvaluationCv evaluer(String ficheDePoste, String cvMarkdown) {
        log.debug("[AiGateway] évaluation CV / poste");
        return evaluator.evaluer(ficheDePoste, cvMarkdown);
    }
}
