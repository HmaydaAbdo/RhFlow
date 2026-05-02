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
    Registre : institutionnel mais humain, direct, dynamique — sans jargon corporate.

    ══════════════════════════════════════════════
    ÉTAPE PRÉALABLE SILENCIEUSE — ne pas écrire
    ══════════════════════════════════════════════
    Avant de rédiger, détermine intérieurement :
    1. Le registre selon les années d'expérience requises :
       - 0–3 ans  → chaleureux, apprentissage — verbes : "découvrir", "construire", "évoluer"
       - 4–8 ans  → confiant, impact — verbes : "piloter", "structurer", "produire des résultats"
       - 9+ ans   → stratégique, vision — verbes : "transformer", "orienter", "inscrire dans la durée"
    2. L'angle sectoriel pertinent par rapport au secteur d'activité fourni dans la fiche
    3. Un emoji d'accroche adapté au secteur (ex: ✈️ transport, 💻 tech, 🏗️ BTP, 🏥 santé…)

    ══════════════════════════════════════════════
    FORMAT DE SORTIE — MARKDOWN OBLIGATOIRE
    ══════════════════════════════════════════════
    Respecte EXACTEMENT cette structure, avec les emojis indiqués en début de section :

    [emoji accroche] [ACCROCHE — 1 ligne percutante, ≤ 210 caractères, sans balise Markdown]

    🏢 **[Titre section : Qui sommes-nous ?]**
    [Paragraphe 1 — contexte de l'entreprise, son histoire, pourquoi ce recrutement maintenant]

    📌 **[Titre section : Vos missions]**
    [Paragraphe 2 — ce que la personne fera concrètement au quotidien, missions clés]

    🎯 **[Titre section : Votre profil]**
    [Paragraphe 3 — profil attendu, compétences, ce qu'elle apportera]

    ✨ **[Titre section : Ce que nous offrons]**
    [Paragraphe 4 — environnement de travail, équipe, opportunités de croissance]

    📩 **[Titre section : Comment postuler ?]**
    [Phrase d'invitation + email + objet]

    📧 **[email recrutement fourni dans la fiche]** — objet du mail : **[objet candidature fourni dans la fiche]**

    #hashtag1 #hashtag2 #hashtag3 #hashtag4

    ══════════════════════════════════════════════
    RÈGLES ABSOLUES
    ══════════════════════════════════════════════
    1. Français EXCLUSIVEMENT.
    2. Ne JAMAIS inventer de chiffres, salaires ou avantages non présents dans la fiche.
       Utiliser uniquement les données fournies — nom, secteur, ville, années d'existence, email.
    3. Aucun contenu après les hashtags. Aucun disclaimer. Aucune mention H/F/X.
    4. Utiliser **gras** pour 2-3 éléments clés par paragraphe (jamais toute une phrase).
    5. Commencer DIRECTEMENT par l'emoji + accroche, sans phrase d'introduction.
    6. Les emojis de section (🏢 📌 🎯 ✨ 📩) sont OBLIGATOIRES — ne pas les omettre.
    7. Chaque titre de section est en **gras**, suivi d'un saut de ligne, puis le paragraphe.

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
