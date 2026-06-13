package com.hrflow.besoinrecrutement.controller;

import com.hrflow.besoinrecrutement.dto.*;
import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.model.StatutBesoin;
import com.hrflow.besoinrecrutement.service.BesoinPdfExportService;
import com.hrflow.besoinrecrutement.service.BesoinRecrutementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/besoins-recrutement")
public class BesoinRecrutementController {

    private final BesoinRecrutementService besoinService;
    private final BesoinPdfExportService   pdfExportService;

    public BesoinRecrutementController(BesoinRecrutementService besoinService,
                                       BesoinPdfExportService pdfExportService) {
        this.besoinService    = besoinService;
        this.pdfExportService = pdfExportService;
    }

    /**
     * Liste paginée avec filtres.
     * - DIRECTEUR : voit uniquement les besoins de ses directions
     * - DRH / ADMIN : voit tout
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @GetMapping
    public ResponseEntity<Page<BesoinRecrutementSummaryResponse>> search(
            @RequestParam(required = false) Long directionId,
            @RequestParam(required = false) Long ficheDePosteId,
            @RequestParam(required = false) StatutBesoin statut,
            @RequestParam(required = false) PrioriteBesoin priorite,
            @RequestParam(required = false) Boolean encours,
            @RequestParam(defaultValue = "false") boolean mineOnly,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        BesoinRecrutementSearchDto search = new BesoinRecrutementSearchDto(
                directionId, ficheDePosteId, statut, priorite, encours, mineOnly);
        return ResponseEntity.ok(besoinService.search(search, pageable));
    }

    /**
     * Détail complet d'un besoin — inclut la fiche de poste complète.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<BesoinRecrutementDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(besoinService.findById(id));
    }

    /**
     * Statistiques dashboard — DRH / ADMIN uniquement.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @GetMapping("/stats")
    public ResponseEntity<BesoinStatsResponse> getStats() {
        return ResponseEntity.ok(besoinService.getStats());
    }

    /**
     * Création d'un besoin — DIRECTEUR (+ DRH/ADMIN qui peuvent aussi en créer).
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @PostMapping
    public ResponseEntity<BesoinRecrutementResponse> create(
            @Valid @RequestBody BesoinRecrutementRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(besoinService.create(request));
    }

    /**
     * Modification d'un besoin — uniquement si encours=true.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @PutMapping("/{id}")
    public ResponseEntity<BesoinRecrutementResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BesoinRecrutementRequest request) {
        return ResponseEntity.ok(besoinService.update(id, request));
    }

    /**
     * Décision DRH : ACCEPTE ou REFUSE — DRH / ADMIN uniquement.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @PatchMapping("/{id}/decision")
    public ResponseEntity<BesoinRecrutementResponse> decider(
            @PathVariable Long id,
            @Valid @RequestBody DecisionBesoinRequest request) {
        return ResponseEntity.ok(besoinService.decider(id, request));
    }

    /**
     * Suppression d'un besoin — DRH / ADMIN uniquement.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        besoinService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /besoins-recrutement/{id}/export-pdf
     * Génère la « Note de présentation à la DG » avec 3 signatures (DRH, DG, Directeur).
     * Accès : ADMIN / DRH uniquement. Disponible quel que soit le statut du besoin
     * (EN_COURS, ACCEPTE, REFUSE, ARCHIVE) — le document peut être ré-généré pour archive.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @PostMapping("/{id}/export-pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Long id,
            @RequestBody BesoinPdfExportRequest request) {

        byte[] pdf = pdfExportService.generate(id, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("besoin-recrutement-" + id + ".pdf")
                        .build());
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

}
