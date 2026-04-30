package com.hrflow.ai.service;

import com.hrflow.ai.config.AiFallbackProperties;
import com.hrflow.ai.utils.JobDescriptionContext;
import com.hrflow.projetrecrutement.exception.ProjetRecrutementNotFoundException;
import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import com.hrflow.projetrecrutement.repositories.ProjetRecrutementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiOfferService {

    private static final Logger log = LoggerFactory.getLogger(AiOfferService.class);

    private final ProjetRecrutementRepository  projetRepository;
    private final JobAnnouncementGenerator     generator;
    private final AiFallbackProperties.CompanyProfile company;

    public AiOfferService(ProjetRecrutementRepository projetRepository,
                          JobAnnouncementGenerator generator,
                          AiFallbackProperties aiProperties) {
        this.projetRepository = projetRepository;
        this.generator        = generator;
        this.company          = aiProperties.company();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 1 — Validate projet OUVERT + build prompt.
    //           Runs inside a transaction so that lazy associations are
    //           fully resolved before handing off to the AI layer.
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String buildValidatedPrompt(Long projetId) {
        ProjetRecrutement projet = projetRepository.findWithDetailsById(projetId)
                .orElseThrow(() -> new ProjetRecrutementNotFoundException(projetId));



        log.info("AI offer generation requested — projet id={}, poste='{}', objetCandidature='{}', company='{}'",
                projetId,
                projet.getFicheDePoste().getIntitulePoste(),
                projet.getObjetCandidature(),
                company.nom());

        return JobDescriptionContext.from(projet, company).toPromptText();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 2 — Synchronous generation (simple request/response).
    // ─────────────────────────────────────────────────────────────────────────

    public String generateSync(String prompt) {
        return generator.generate(prompt);
    }
}
