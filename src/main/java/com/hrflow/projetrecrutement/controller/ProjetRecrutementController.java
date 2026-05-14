package com.hrflow.projetrecrutement.controller;

import com.hrflow.projetrecrutement.dto.ProjetPdfExportRequest;
import com.hrflow.projetrecrutement.dto.ProjetRecrutementResponse;
import com.hrflow.projetrecrutement.dto.ProjetRecrutementSearchDto;
import com.hrflow.projetrecrutement.dto.ProjetRecrutementSummaryResponse;
import com.hrflow.projetrecrutement.dto.UpdateObjetCandidatureRequest;
import com.hrflow.projetrecrutement.model.StatutProjet;
import com.hrflow.projetrecrutement.service.ProjetPdfExportService;
import com.hrflow.projetrecrutement.service.ProjetRecrutementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projets-recrutement")
public class ProjetRecrutementController {

    private final ProjetRecrutementService projetService;
    private final ProjetPdfExportService   pdfExportService;

    public ProjetRecrutementController(ProjetRecrutementService projetService,
                                       ProjetPdfExportService pdfExportService) {
        this.projetService    = projetService;
        this.pdfExportService = pdfExportService;
    }

    /**
     * GET /projets-recrutement
     * Accès : ADMIN, DRH, DIRECTEUR (scope appliqué côté service)
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @GetMapping
    public ResponseEntity<Page<ProjetRecrutementSummaryResponse>> search(
            @RequestParam(required = false) Long          directionId,
            @RequestParam(required = false) Long          ficheDePosteId,
            @RequestParam(required = false) StatutProjet  statut,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "9")   int size,
            @RequestParam(defaultValue = "createdAt")  String sortBy,
            @RequestParam(defaultValue = "desc")       String direction) {

        ProjetRecrutementSearchDto search = new ProjetRecrutementSearchDto(
                directionId, ficheDePosteId, statut);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(direction), sortBy));

        return ResponseEntity.ok(projetService.search(search, pageable));
    }

    /**
     * GET /projets-recrutement/{id}
     * Accès : ADMIN, DRH, DIRECTEUR (périmètre vérifié côté service)
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<ProjetRecrutementResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.findById(id));
    }

    /**
     * POST /projets-recrutement/{id}/export-pdf
     * Génère un document PDF narratif pour la DG — ADMIN / DRH uniquement.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @PostMapping("/{id}/export-pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Long id,
            @RequestBody ProjetPdfExportRequest request) {

        byte[] pdf = pdfExportService.generate(id, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("recrutement-" + id + ".pdf")
                        .build());
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /**
     * PATCH /projets-recrutement/{id}/fermer
     * Accès : ADMIN, DRH uniquement
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @PatchMapping("/{id}/fermer")
    public ResponseEntity<ProjetRecrutementResponse> fermer(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.fermer(id));
    }

    /**
     * PATCH /projets-recrutement/{id}/objet-candidature
     * Permet à un DRH/ADMIN de personnaliser l'objet de candidature d'un projet.
     * Accès : ADMIN, DRH uniquement
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @PatchMapping("/{id}/objet-candidature")
    public ResponseEntity<ProjetRecrutementResponse> updateObjetCandidature(
            @PathVariable Long id,
            @Valid @RequestBody UpdateObjetCandidatureRequest request) {
        return ResponseEntity.ok(projetService.updateObjetCandidature(id, request));
    }
}
