package com.hrflow.candidature.model;

/**
 * Lifecycle d'une candidature.
 *
 * RECU      → fichier uploadé, traitement IA pas encore lancé
 * EN_COURS  → extraction + évaluation IA en cours (async)
 * EVALUE    → IA terminée, score disponible
 * RETENU    → DRH/ADMIN a retenu le candidat
 * REJETE    → DRH/ADMIN a rejeté le candidat
 * ERREUR    → le traitement IA a échoué (fichier illisible, timeout…)
 */
public enum StatutCandidature {
    RECU,
    EN_COURS,
    EVALUE,
    ERREUR,
    RETENU,
    REJETE
}
