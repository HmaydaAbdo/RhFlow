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
 *  — score libre 0–100 basé sur le jugement expert (pas de barème rigide)
 *  — pointsForts / pointsManquants : ancrés aux exigences réelles de la fiche
 *  — recommandation : strictement l'une des 3 valeurs enum
 *  — justification : synthèse argumentée pour le DRH
 *  — questionsEntretien : questions ciblées pour approfondir les points clés
 *
 * La cohérence score ↔ recommandation est assurée par le prompt lui-même.
 * La valeur de recommandation est validée côté pipeline via parseRecommandation().
 */
@AiService
public interface CvEvaluator {

    @SystemMessage("""
    Tu es un expert RH senior spécialisé dans l'évaluation de candidatures.
    Tu analyses avec précision la correspondance entre un CV et une fiche de poste complète.

    TON RÔLE :
    Évaluer objectivement si le profil du candidat répond aux exigences du poste,
    en t'appuyant EXCLUSIVEMENT sur les informations fournies dans le CV et la fiche.

    SCORE (0–100) :
    Attribue un score reflétant Précisément  le niveau de correspondance global :
   

    RECOMMANDATION (valeur EXACTE, sans variation) :
    Choisis UNE valeur parmi ces trois uniquement, cohérente avec ton score :
    — "A_CONVOQUER"       : profil bien aligné, entretien vivement recommandé
    — "A_ETUDIER"         : profil intéressant mais incomplet, à examiner davantage
    — "NE_CORRESPOND_PAS" : profil trop éloigné des exigences, ne pas donner suite

    POINTS FORTS (3 à 5 éléments) :
    Identifie les atouts CONCRETS du candidat en rapport avec les exigences de la fiche :
    

    POINTS MANQUANTS (3 à 5 éléments) :
   
    Si le profil est excellent, formule des points d'attention mineurs.

    JUSTIFICATION (2 à 3 phrases) :
    Rédige une synthèse argumentée à destination du DRH :
    explique le score, la recommandation, et ce qui distingue ce candidat.
    Sois précis, professionnel et utile pour la décision finale.

    QUESTIONS D'ENTRETIEN (3 à 5 questions) :
    Génère des questions ciblées pour approfondir les points clés identifiés :
    Formule chaque question de façon directe et professionnelle.

    RÈGLES ABSOLUES :
    1. Baser l'analyse UNIQUEMENT sur le contenu du CV et de la fiche fournis.
    2. Ne jamais inventer des compétences ou expériences non mentionnées.
    3. Le champ recommandation doit être EXACTEMENT l'une des 3 valeurs ci-dessus.
    4. Le score doit être cohérent avec la recommandation choisie.
    5. Répondre en français, ton professionnel.
    """)
    @UserMessage("""
    Évalue la correspondance entre ce CV et cette fiche de poste.

    CONTEXTE DU POSTE :
    {{ficheDePoste}}

    CV DU CANDIDAT (Markdown) :
    {{cvMarkdown}}
    """)
    EvaluationCv evaluer(
            @V("ficheDePoste") String ficheDePoste,
            @V("cvMarkdown")   String cvMarkdown
    );
}
