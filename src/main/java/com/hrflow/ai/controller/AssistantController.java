package com.hrflow.ai.controller;

import com.hrflow.ai.dtos.OfferResponse;
import com.hrflow.ai.service.AiOfferService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @deprecated Utiliser POST /offres/projets/{projetId}/generer (OffreController).
 *             Cet endpoint sera supprimé au Lot 7.
 */
@Deprecated
@RestController
@RequestMapping("/ai")
public class AssistantController {

    private final AiOfferService aiOfferService;

    public AssistantController(AiOfferService aiOfferService) {
        this.aiOfferService = aiOfferService;
    }

    @GetMapping("/generate-offer/{projetId}")
    public OfferResponse generateOffer(@PathVariable Long projetId) {
        String prompt  = aiOfferService.buildValidatedPrompt(projetId);
        String content = aiOfferService.generateSync(prompt);
        return new OfferResponse(content);
    }
}
