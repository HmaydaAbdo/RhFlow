package com.hrflow.ai.service;

import com.hrflow.ai.dto.CandidatInfo;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * Extraction des informations du candidat à partir du texte Markdown de son CV.
 *
 * Retourne un {@link CandidatInfo} avec coordonnées, formations et expériences
 * professionnelles. Les détails contractuels (format de chaque champ, règle
 * « null si absent ») sont portés par les {@code @Description} sur le record
 * et ses sous-records — ces descriptions sont injectées dans le schéma JSON
 * envoyé au LLM, donc inutile de les répéter dans le prompt.
 *
 * LangChain4j génère le schéma depuis le record (y compris les records imbriqués
 * Formation / ExperienceProfessionnelle) et force la sortie structurée.
 */
@AiService
public interface CvDataExtractor {

    @SystemMessage("""
    Tu es un extracteur d'informations de CV. Ton seul rôle est d'extraire,
    depuis le texte du CV fourni, les coordonnées du candidat, ses formations
    et ses expériences professionnelles.

    RÈGLES ABSOLUES :
    1. Extraire UNIQUEMENT ce qui est explicitement écrit dans le CV.
    2. Ne JAMAIS inventer, déduire, deviner ou compléter une information absente.
    3. Si une information scalaire est absente ou ambiguë → retourner null.
    4. Si une rubrique entière est absente (aucune formation, aucune expérience)
       → retourner une liste vide pour cette rubrique.
    5. Préserver l'ordre des formations et expériences tel qu'il apparaît dans le CV.

    Le format précis attendu pour chaque champ est décrit par le schéma de sortie
    (descriptions associées à chaque propriété). Respecte-le strictement.
    """)
    @UserMessage("""
    Extrait les informations du candidat depuis le CV suivant.

    CV (Markdown) :
    {{cvMarkdown}}
    """)
    CandidatInfo extraire(
            @V("cvMarkdown") String cvMarkdown
    );
}
