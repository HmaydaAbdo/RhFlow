package com.hrflow.ai.dto;

import java.util.List;

/**
 * Résultat de l'évaluation d'un CV par rapport à une fiche de poste.
 *
 * LangChain4j génère automatiquement le schéma JSON depuis ce record et
 * force le LLM à répondre avec une structure strictement typée.
 *
 * @param scoreMatching   Score global de correspondance, de 0 (aucun lien) à 100 (parfait).
 * @param pointsForts     Atouts du candidat correspondant au poste — 3 à 5 éléments.
 *                        Stocké en JSON TEXT dans la base après sérialisation par le pipeline.
 * @param pointsManquants Lacunes par rapport aux exigences du poste — 3 à 5 éléments.
 *                        Stocké en JSON TEXT dans la base après sérialisation par le pipeline.
 * @param recommandation  Décision IA : "A_CONVOQUER", "A_ETUDIER" ou "NE_CORRESPOND_PAS".
 *                        Validée et corrigée par le pipeline si incohérente avec le score.
 * @param justificationIa Explication synthétique du score et de la décision (2–3 phrases).
 */
public record EvaluationCv(
        int          scoreMatching,
        List<String> pointsForts,
        List<String> pointsManquants,
        String       recommandation,
        String       justificationIa
) {}
