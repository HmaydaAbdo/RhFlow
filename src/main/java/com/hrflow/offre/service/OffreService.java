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

    // ── Générer (ou régénérer) — DRH / ADMIN ─────────────────────────────────────

    /**
     * FIX #2 — Connexion DB libérée pendant l'appel LLM.
     *
     * Pattern : deux transactions courtes séparées par l'appel IA (hors transaction).
     *
     *  TX 1 (readOnly) : charger projet + vérifications métier → connexion libérée
     *  [appel LLM — 5 à 30 s — AUCUNE connexion tenue]
     *  TX 2 (write)    : upsert Offre → connexion prise le temps du save()
     *
     * Sans ce pattern, une seule @Transactional tenait la connexion pendant toute
     * la durée du LLM → pool exhaustion sous charge.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','DRH')")
    public OffreResponse generer(Long projetId) {

        // ── TX 1 : lecture + validation (connexion libérée à la fin du bloc) ──────
        String prompt = chargerEtValider(projetId);

        // ── Hors transaction : appel LLM (peut durer 5–30 s) ─────────────────────
        log.info("[Offre] appel LLM pour projet={}", projetId);
        String contenu = aiOfferService.generateSync(prompt);

        // ── TX 2 : persistance ────────────────────────────────────────────────────
        return sauvegarderOffre(projetId, contenu);
    }

    @Transactional(readOnly = true)
    protected String chargerEtValider(Long projetId) {
        ProjetRecrutement projet = projetRepository.findWithDetailsById(projetId)
                .orElseThrow(() -> new ProjetRecrutementNotFoundException(projetId));

        if (projet.getStatut() != StatutProjet.OUVERT) {
            throw new OfferGenerationNotAllowedException(
                    "Génération impossible : le projet %d n'est pas OUVERT (statut : %s)"
                            .formatted(projetId, projet.getStatut()));
        }
        return aiOfferService.buildValidatedPrompt(projetId);
    }

    @Transactional
    protected OffreResponse sauvegarderOffre(Long projetId, String contenu) {
        ProjetRecrutement projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ProjetRecrutementNotFoundException(projetId));

        Offre offre = offreRepository.findByProjetRecrutementId(projetId)
                .orElseGet(Offre::new);
        offre.setProjetRecrutement(projet);
        offre.setContenu(contenu);

        Offre saved = offreRepository.save(offre);
        log.info("[Offre] {} pour projet={}, poste='{}'",
                offre.getId() == null ? "créée" : "régénérée",
                projetId,
                projet.getFicheDePoste().getIntitulePoste());

        return offreMapper.toResponse(saved);
    }

    // ── Read par projet — DRH / ADMIN / DIRECTEUR ────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ADMIN','DRH','DIRECTEUR')")
    @Transactional(readOnly = true)
    public OffreResponse getByProjetId(Long projetId) {
        Offre offre = offreRepository.findByProjetRecrutementId(projetId)
                .orElseThrow(() -> new OffreNotFoundException(projetId));
        return offreMapper.toResponse(offre);
    }

    // ── Édition manuelle — DRH / ADMIN ───────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ADMIN','DRH')")
    @Transactional
    public OffreResponse update(Long projetId, OffreUpdateRequest request) {
        Offre offre = offreRepository.findByProjetRecrutementId(projetId)
                .orElseThrow(() -> new OffreNotFoundException(projetId));
        offre.setContenu(request.contenu());
        Offre saved = offreRepository.save(offre);
        log.info("[Offre] modifiée manuellement pour projet={}", projetId);
        return offreMapper.toResponse(saved);
    }
}
