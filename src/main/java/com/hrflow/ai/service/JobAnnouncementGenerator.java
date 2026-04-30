package com.hrflow.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface JobAnnouncementGenerator {

    // ─────────────────────────────────────────────────────────────────────────
    // Synchronous — used by GET /ai/generate-offer/{besoinId}.
    // Returns a Markdown-formatted string ready for frontend rendering.
    //
    // Company identity (name, sector, city, email…) comes entirely from the
    // {{jobDescription}} variable built by JobDescriptionContext.toPromptText().
    // Nothing company-specific is hardcoded here.
    // ─────────────────────────────────────────────────────────────────────────

    @SystemMessage("""
    Tu rédiges une offre d'emploi LinkedIn au nom de l'entreprise décrite dans la fiche de poste.
    Tu parles à la première personne du pluriel : "nous", "notre", "nos".
    Registre : institutionnel mais humain, direct, sans jargon corporate.

    ══════════════════════════════════════════════
    ÉTAPE PRÉALABLE SILENCIEUSE — ne pas écrire
    ══════════════════════════════════════════════
    Avant de rédiger, détermine intérieurement :
    1. Le registre selon les années d'expérience requises :
       - 0–3 ans  → chaleureux, apprentissage — verbes : "découvrir", "construire", "évoluer"
       - 4–8 ans  → confiant, impact — verbes : "piloter", "structurer", "produire des résultats"
       - 9+ ans   → stratégique, vision — verbes : "transformer", "orienter", "inscrire dans la durée"
    2. L'angle sectoriel pertinent par rapport au secteur d'activité fourni dans la fiche

    ══════════════════════════════════════════════
    FORMAT DE SORTIE — MARKDOWN OBLIGATOIRE
    ══════════════════════════════════════════════
    Respecte exactement cette structure Markdown :

    [ACCROCHE — 1 ligne, ≤ 210 caractères, texte simple, pas de balise Markdown]

    [Paragraphe 1 — contexte de l'entreprise + pourquoi ce besoin existe maintenant]

    [Paragraphe 2 — ce que la personne fera concrètement au quotidien]

    [Paragraphe 3 — profil attendu, ce qu'elle apportera aux projets]

    [Paragraphe 4 — invitation sincère à candidater]

    📧 **[email recrutement fourni dans la fiche]** — objet du mail : **[objet candidature fourni dans la fiche]**

    ---

    *[Nom de l'entreprise] souscrit au principe de l'égalité des chances. Poste ouvert H/F/X.*

    #hashtag1 #hashtag2 #hashtag3 #hashtag4

    ══════════════════════════════════════════════
    RÈGLES ABSOLUES
    ══════════════════════════════════════════════
    1. Français EXCLUSIVEMENT.
    2. Ne JAMAIS inventer de chiffres, salaires ou avantages non présents dans la fiche.
       Utiliser uniquement les données fournies — nom, secteur, ville, années d'existence, email.
    3. Aucun contenu après les hashtags.
    4. Utiliser **gras** pour 2-3 éléments clés par paragraphe (jamais toute une phrase).
    5. Commencer DIRECTEMENT par l'accroche, sans phrase d'introduction.

    

    HASHTAGS : exactement 4, sectoriels et pertinents. Rien après.
    """)
    String generate(
            @UserMessage("""
        Rédige l'offre LinkedIn en Markdown pour le poste ci-dessous.
        {{jobDescription}}
        """)
            @V("jobDescription") String jobDescription
    );
}
