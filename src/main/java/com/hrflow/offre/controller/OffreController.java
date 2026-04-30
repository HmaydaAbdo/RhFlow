package com.hrflow.offre.controller;

import com.hrflow.offre.dto.OffreResponse;
import com.hrflow.offre.dto.OffreUpdateRequest;
import com.hrflow.offre.service.OffreService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/offres")
public class OffreController {

    private final OffreService offreService;

    public OffreController(OffreService offreService) {
        this.offreService = offreService;
    }

    /**
     * Génère (ou régénère) l'offre IA pour un projet OUVERT.
     * DRH / ADMIN uniquement.
     */
    @PostMapping("/projets/{projetId}/generer")
    public ResponseEntity<OffreResponse> generer(@PathVariable Long projetId) {
        return ResponseEntity.ok(offreService.generer(projetId));
    }

    /**
     * Récupère l'offre existante d'un projet.
     */
    @GetMapping("/projets/{projetId}")
    public ResponseEntity<OffreResponse> getByProjet(@PathVariable Long projetId) {
        return ResponseEntity.ok(offreService.getByProjetId(projetId));
    }

    /**
     * Édition manuelle du contenu Markdown.
     * DRH / ADMIN uniquement.
     */
    @PutMapping("/projets/{projetId}")
    public ResponseEntity<OffreResponse> update(
            @PathVariable Long projetId,
            @Valid @RequestBody OffreUpdateRequest request) {
        return ResponseEntity.ok(offreService.update(projetId, request));
    }
}
