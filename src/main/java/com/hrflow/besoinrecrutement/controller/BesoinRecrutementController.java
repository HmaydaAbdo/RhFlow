package com.hrflow.besoinrecrutement.controller;

import com.hrflow.besoinrecrutement.dto.*;
import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.model.StatutBesoin;
import com.hrflow.besoinrecrutement.service.BesoinRecrutementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/besoins-recrutement")
public class BesoinRecrutementController {

    private final BesoinRecrutementService besoinService;

    public BesoinRecrutementController(BesoinRecrutementService besoinService) {
        this.besoinService = besoinService;
    }

    /**
     * Liste paginée avec filtres.
     * - DIRECTEUR : voit uniquement les besoins de ses directions
     * - DRH / ADMIN : voit tout
     */
    @GetMapping
    public ResponseEntity<Page<BesoinRecrutementSummaryResponse>> search(
            @RequestParam(required = false) Long directionId,
            @RequestParam(required = false) Long ficheDePosteId,
            @RequestParam(required = false) StatutBesoin statut,
            @RequestParam(required = false) PrioriteBesoin priorite,
            @RequestParam(defaultValue = "false") boolean mineOnly,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        BesoinRecrutementSearchDto search = new BesoinRecrutementSearchDto(
                directionId, ficheDePosteId, statut, priorite, mineOnly);
        return ResponseEntity.ok(besoinService.search(search, pageable));
    }

    /**
     * Détail complet d'un besoin.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BesoinRecrutementResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(besoinService.findById(id));
    }

    /**
     * Statistiques dashboard — DRH / ADMIN uniquement (contrôlé dans SecurityConfig).
     */
    @GetMapping("/stats")
    public ResponseEntity<BesoinStatsResponse> getStats() {
        return ResponseEntity.ok(besoinService.getStats());
    }

    /**
     * Création d'un besoin — DIRECTEUR uniquement.
     */
    @PostMapping
    public ResponseEntity<BesoinRecrutementResponse> create(
            @Valid @RequestBody BesoinRecrutementRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(besoinService.create(request));
    }

    /**
     * Modification d'un besoin — DIRECTEUR (propriétaire) uniquement.
     * Autorisé quel que soit le statut courant.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BesoinRecrutementResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BesoinRecrutementRequest request) {
        return ResponseEntity.ok(besoinService.update(id, request));
    }

    /**
     * Décision DRH : ACCEPTE ou REFUSE avec motif optionnel.
     */
    @PatchMapping("/{id}/decision")
    public ResponseEntity<BesoinRecrutementResponse> decider(
            @PathVariable Long id,
            @Valid @RequestBody DecisionBesoinRequest request) {
        return ResponseEntity.ok(besoinService.decider(id, request));
    }

    /**
     * Suppression d'un besoin — DIRECTEUR (propriétaire) uniquement.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        besoinService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
