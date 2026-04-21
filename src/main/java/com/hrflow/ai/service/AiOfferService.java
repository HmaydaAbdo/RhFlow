package com.hrflow.ai.service;

import com.hrflow.ai.utils.JobDescriptionContext;
import com.hrflow.ai.config.AiFallbackProperties;
import com.hrflow.ai.exception.OfferGenerationNotAllowedException;
import com.hrflow.besoinrecrutement.exception.BesoinRecrutementNotFoundException;
import com.hrflow.besoinrecrutement.model.BesoinRecrutement;
import com.hrflow.besoinrecrutement.model.StatutBesoin;
import com.hrflow.besoinrecrutement.repositories.BesoinRecrutementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiOfferService {

    private static final Logger log = LoggerFactory.getLogger(AiOfferService.class);

    private final BesoinRecrutementRepository         besoinRepository;
    private final JobAnnouncementGenerator generator;
    private final AiFallbackProperties.CompanyProfile company;

    public AiOfferService(BesoinRecrutementRepository besoinRepository,
                          JobAnnouncementGenerator generator,
                          AiFallbackProperties aiProperties) {
        this.besoinRepository = besoinRepository;
        this.generator        = generator;
        this.company          = aiProperties.company();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 1 — Validate + build prompt (runs inside a transaction so that
    //           lazy associations on BesoinRecrutement are fully resolved).
    //           Throws before any emitter is created so GlobalExceptionHandler
    //           can return a clean HTTP error response.
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String buildValidatedPrompt(Long besoinId) {
        BesoinRecrutement besoin = besoinRepository.findWithDetailsById(besoinId)
                .orElseThrow(() -> new BesoinRecrutementNotFoundException(besoinId));

        log.info("AI offer generation requested — besoin id={}, poste='{}', company='{}'",
                besoinId, besoin.getFicheDePoste().getIntitulePoste(), company.nom());

        return JobDescriptionContext.from(besoin, company).toPromptText();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 2a — Synchronous generation (simple request/response).
    // ─────────────────────────────────────────────────────────────────────────
    public String generateSync(String prompt) {
        return generator.generate(prompt);
    }
}
