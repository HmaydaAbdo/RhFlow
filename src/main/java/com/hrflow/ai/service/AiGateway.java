package com.hrflow.ai.service;

import com.hrflow.ai.dto.CandidatInfo;
import com.hrflow.ai.dto.EvaluationCv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Façade retryable sur les services IA LangChain4j.
 *
 * Problème résolu : les beans {@code @AiService} de LangChain4j sont des proxys JDK
 * (interfaces). Appliquer {@code @Retryable} directement sur une interface crée un
 * double-proxying fragile. Cette façade concrète encapsule les deux interfaces et expose
 * des méthodes sur lesquelles Spring Retry peut créer un proxy CGLIB stable.
 *
 * Politique de retry :
 *  — maxAttempts = 3 (1 tentative initiale + 2 retries)
 *  — backoff exponentiel : 1 s → 2 s (couvre quota HTTP 429 et timeouts transitoires)
 *  — retryFor = RuntimeException.class (LangChain4j encapsule toutes les erreurs réseau
 *    et API dans RuntimeException ; les IllegalArgumentException ne sont pas retriées)
 *
 * En cas d'échec définitif (3 tentatives), l'exception est propagée au pipeline qui
 * marque la candidature ERREUR via {@code markErreur()}.
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
     * @param cvMarkdown texte Markdown du CV (issu de Docling)
     * @return coordonnées extraites (champs null si absents du CV)
     * @throws RuntimeException si le provider LLM est inaccessible après 3 tentatives
     */
    @Retryable(
        retryFor    = RuntimeException.class,
        noRetryFor  = IllegalArgumentException.class,
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
     * @param ficheDePoste description textuelle de la fiche de poste
     * @param cvMarkdown   texte Markdown du CV
     * @return évaluation (score, points forts/manquants, recommandation, justification)
     * @throws RuntimeException si le provider LLM est inaccessible après 3 tentatives
     */
    @Retryable(
        retryFor    = RuntimeException.class,
        noRetryFor  = IllegalArgumentException.class,
        maxAttempts = 3,
        backoff     = @Backoff(delay = 1000, multiplier = 2, maxDelay = 8000)
    )
    public EvaluationCv evaluer(String ficheDePoste, String cvMarkdown) {
        log.debug("[AiGateway] évaluation CV / poste");
        return evaluator.evaluer(ficheDePoste, cvMarkdown);
    }
}
