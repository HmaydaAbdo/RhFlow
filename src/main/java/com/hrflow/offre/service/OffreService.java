package com.hrflow.offre.service;

import com.hrflow.ai.exception.OfferGenerationNotAllowedException;
import com.hrflow.ai.service.AiOfferService;
import com.hrflow.offre.dto.OffreResponse;
import com.hrflow.offre.dto.OffreUpdateRequest;
import com.hrflow.offre.exception.OffreNotFoundException;
import com.hrflow.offre.mapper.OffreMapper;
import com.hrflow.offre.model.Offre;
import com.hrflow.offre.repositories.OffreRepository;
import com.hrflow.projetrecrutement.exception.ProjetRecrutementNotFoundException;
import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import com.hrflow.projetrecrutement.model.StatutProjet;
import com.hrflow.projetrecrutement.repositories.ProjetRecrutementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OffreService {

    private static final Logger log = LoggerFactory.getLogger(OffreService.class);

    private final OffreRepository             offreRepository;
    private final OffreMapper                 offreMapper;
    private final ProjetRecrutementRepository projetRepository;
    private final AiOfferService              aiOfferService;

    public OffreService(OffreRepository offreRepository,
                        OffreMapper offreMapper,
                        ProjetRecrutementRepository projetRepository,
                        AiOfferService aiOfferService) {
        this.offreRepository  = offreRepository;
        this.offreMapper      = offreMapper;
        this.projetRepository = projetRepository;
        this.aiOfferService   = aiOfferService;
    }

    // =====================================================================
    // GÉNÉRER (ou régénérer) — DRH / ADMIN
    // =====================================================================

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @Transactional
    public OffreResponse generer(Long projetId) {
        ProjetRecrutement projet = projetRepository.findWithDetailsById(projetId)
                .orElseThrow(() -> new ProjetRecrutementNotFoundException(projetId));

        if (projet.getStatut() != StatutProjet.OUVERT) {
            throw new OfferGenerationNotAllowedException(
                    "Génération impossible : le projet %d n'est pas OUVERT (statut : %s)"
                            .formatted(projetId, projet.getStatut()));
        }

        // Génération IA (hors transaction pour ne pas tenir la connexion pendant l'appel LLM)
        String prompt  = aiOfferService.buildValidatedPrompt(projetId);
        String contenu = aiOfferService.generateSync(prompt);

        // Upsert : on écrase l'offre existante si elle existe déjà
        Offre offre = offreRepository.findByProjetRecrutementId(projetId)
                .orElseGet(Offre::new);

        offre.setProjetRecrutement(projet);
        offre.setContenu(contenu);

        Offre saved = offreRepository.save(offre);
        log.info("Offre {} pour projet id={}, poste='{}'",
                offre.getId() == null ? "créée" : "régénérée",
                projetId,
                projet.getFicheDePoste().getIntitulePoste());

        return offreMapper.toResponse(saved);
    }

    // =====================================================================
    // READ par projet — DRH / ADMIN / DIRECTEUR
    // =====================================================================

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @Transactional(readOnly = true)
    public OffreResponse getByProjetId(Long projetId) {
        Offre offre = offreRepository.findByProjetRecrutementId(projetId)
                .orElseThrow(() -> new OffreNotFoundException(projetId));
        return offreMapper.toResponse(offre);
    }

    // =====================================================================
    // ÉDITION MANUELLE — DRH / ADMIN
    // =====================================================================

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @Transactional
    public OffreResponse update(Long projetId, OffreUpdateRequest request) {
        Offre offre = offreRepository.findByProjetRecrutementId(projetId)
                .orElseThrow(() -> new OffreNotFoundException(projetId));

        offre.setContenu(request.contenu());
        Offre saved = offreRepository.save(offre);

        log.info("Offre modifiée manuellement pour projet id={}", projetId);
        return offreMapper.toResponse(saved);
    }
}
