package com.hrflow.candidature.controller;

import com.hrflow.candidature.dto.CandidatureResponse;
import com.hrflow.candidature.dto.CandidatureSearchDto;
import com.hrflow.candidature.dto.StatutUpdateRequest;
import com.hrflow.candidature.model.RecommandationIA;
import com.hrflow.candidature.model.StatutCandidature;
import com.hrflow.candidature.service.CandidatureService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/candidatures")
public class CandidatureController {

    private final CandidatureService service;

    public CandidatureController(CandidatureService service) {
        this.service = service;
    }

    // ── Upload CV ─────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @PostMapping("/projets/{projetId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CandidatureResponse upload(
            @PathVariable Long projetId,
            @RequestParam("file") MultipartFile file) {
        return service.upload(projetId, file);
    }

    // ── Liste paginée + filtrée ───────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @GetMapping("/projets/{projetId}")
    public ResponseEntity<Page<CandidatureResponse>> lister(
            @PathVariable Long projetId,
            @RequestParam(required = false) StatutCandidature statut,
            @RequestParam(required = false) RecommandationIA  recommandation,
            @RequestParam(required = false) Integer           scoreMin,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        CandidatureSearchDto search = new CandidatureSearchDto(statut, recommandation, scoreMin);
        return ResponseEntity.ok(service.listerParProjet(projetId, search, page, size));
    }

    // ── Détail ────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<CandidatureResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // ── Changer statut (RETENU / REJETE) — DRH / ADMIN ───────────────────────

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @PatchMapping("/{id}/statut")
    public ResponseEntity<CandidatureResponse> changerStatut(
            @PathVariable Long id,
            @Valid @RequestBody StatutUpdateRequest request) {
        return ResponseEntity.ok(service.changerStatut(id, request));
    }

    // ── Re-évaluation IA — DRH / ADMIN ───────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @PostMapping("/{id}/reevaluer")
    public ResponseEntity<CandidatureResponse> reevaluer(@PathVariable Long id) {
        return ResponseEntity.ok(service.reevaluer(id));
    }

    // ── Presigned URL (accès au fichier CV) ──────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH', 'DIRECTEUR')")
    @GetMapping("/{id}/cv")
    public ResponseEntity<Map<String, String>> getCvUrl(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("url", service.presignedUrl(id)));
    }

    // ── Suppression — DRH / ADMIN ─────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }
}
