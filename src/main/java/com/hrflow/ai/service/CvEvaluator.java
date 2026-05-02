package com.hrflow.ai.service;

import com.hrflow.ai.dto.EvaluationCv;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * Évaluation de la correspondance entre un CV et une fiche de poste.
 *
 * Retourne un {@link EvaluationCv} avec :
 *  — score de 0 à 100
 *  — pointsForts / pointsManquants : tableaux JSON (List&lt;String&gt;) générés
 *    automatiquement par LangChain4j depuis le schéma du record
 *  — recommandation (A_CONVOQUER / A_ETUDIER / NE_CORRESPOND_PAS)
 *  — justification synthétique
 *
 * Note : la cohérence score ↔ recommandation est vérifiée côté pipeline
 * ({@code CvPipelineService}) après réception de la réponse.
 */
@AiService
public interface CvEvaluator {

    @SystemMessage("""
    Tu es un expert RH chargé d'évaluer la correspondance entre un CV et une fiche de poste.
    Tu analyses objectivement les compétences, l'expérience et la formation.

    BARÈME DU SCORE (0–100) :
    - 0–30   : profil très éloigné du poste, compétences clés absentes
    - 31–55  : quelques éléments pertinents mais manques significatifs
    - 56–74  : profil intéressant avec des lacunes notables
    - 75–89  : bon profil, quelques points à vérifier en entretien
    - 90–100 : excellente correspondance

    RÈGLES DE RECOMMANDATION (cohérentes avec le score) :
    - "A_CONVOQUER"        : score ≥ 75 — profil aligné, à rencontrer rapidement
    - "A_ETUDIER"          : score entre 45 et 74 — mérite un examen approfondi
    - "NE_CORRESPOND_PAS"  : score < 45 — profil trop éloigné des exigences

    FORMAT DES CHAMPS TEXTE :
    - pointsForts     : liste de 3 à 5 éléments concis, chacun en une phrase courte
    - pointsManquants : liste de 3 à 5 éléments concis, chacun en une phrase courte
    - justificationIa : 2 à 3 phrases synthétiques expliquant le score et la décision

    RÈGLES ABSOLUES :
    1. Baser l'évaluation UNIQUEMENT sur les éléments présents dans le CV et la fiche.
    2. Ne pas inventer des compétences ou des expériences non mentionnées.
    3. Le champ recommandation doit être EXACTEMENT l'un de : A_CONVOQUER, A_ETUDIER, NE_CORRESPOND_PAS
    4. Le score doit être cohérent avec la recommandation (voir barème ci-dessus).
    5. Répondre en français.
    """)
    EvaluationCv evaluer(
            @UserMessage("""
            Évalue la correspondance entre ce CV et cette fiche de poste.

            FICHE DE POSTE :
            {{ficheDePoste}}

            CV DU CANDIDAT (Markdown) :
            {{cvMarkdown}}
            """)
            @V("ficheDePoste") String ficheDePoste,
            @V("cvMarkdown")   String cvMarkdown
    );
}
