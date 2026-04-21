package com.hrflow.ai.controller;

import com.hrflow.ai.dtos.OfferResponse;
import com.hrflow.ai.service.AiOfferService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AssistantController {

    private final AiOfferService aiOfferService;

    public AssistantController(AiOfferService aiOfferService) {

        this.aiOfferService           = aiOfferService;
    }

    // ── Legacy raw text endpoint ─────────────────────────────────────────────


    // ── Primary endpoint — validates besoin ACCEPTE, returns Markdown JSON ───

    @GetMapping("/generate-offer/{besoinId}")
    public OfferResponse generateOffer(@PathVariable Long besoinId) {
        String prompt  = aiOfferService.buildValidatedPrompt(besoinId);
        String content = aiOfferService.generateSync(prompt);
        return new OfferResponse(content);
    }


}
