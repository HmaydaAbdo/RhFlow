package com.hrflow.ai.service;

import com.hrflow.ai.dto.CandidatInfo;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * Extraction des coordonnées du candidat à partir du texte Markdown de son CV.
 *
 * Retourne un {@link CandidatInfo} avec les champs trouvés dans le document.
 * Les champs non trouvés sont null — jamais inventés.
 *
 * LangChain4j génère automatiquement le schéma JSON et force la sortie structurée.
 */
@AiService
public interface CvDataExtractor {

    @SystemMessage("""
    Tu es un extracteur d'informations de CV. Ton seul rôle est d'extraire
    les coordonnées du candidat depuis le texte du CV fourni.

    RÈGLES STRICTES :
    1. Extraire UNIQUEMENT ce qui est explicitement écrit dans le CV.
    2. Ne jamais inventer, déduire ou compléter une information absente.
    3. Si une information est absente ou ambiguë, retourner null pour ce champ.
    4. Pour l'email : format xxx@xxx.xx uniquement.
    5. Pour le téléphone : conserver le format original du CV (avec indicatif si présent).
    6. Pour le nom : prénom + nom complet, tel qu'écrit dans le CV.
    """)
    CandidatInfo extraire(
            @UserMessage("""
            Extrait les coordonnées du candidat depuis le CV suivant.

            CV (Markdown) :
            {{cvMarkdown}}
            """)
            @V("cvMarkdown") String cvMarkdown
    );
}
