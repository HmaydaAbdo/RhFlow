package com.hrflow.projetrecrutement.controller;

import com.hrflow.projetrecrutement.dto.ProjetRecrutementResponse;
import com.hrflow.projetrecrutement.dto.ProjetRecrutementSearchDto;
import com.hrflow.projetrecrutement.dto.ProjetRecrutementSummaryResponse;
import com.hrflow.projetrecrutement.model.StatutProjet;
import com.hrflow.projetrecrutement.service.ProjetRecrutementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projets-recrutement")
public class ProjetRecrutementController {

    private final ProjetRecrutementService projetService;

    public ProjetRecrutementController(ProjetRecrutementService projetService) {
        this.projetService = projetService;
    }

    /**
     * GET /projets-recrutement
     * Accès : ADMIN, DRH, DIRECTEUR (scope appliqué côté service)
     */
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
    @GetMapping("/{id}")
    public ResponseEntity<ProjetRecrutementResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.findById(id));
    }

    /**
     * PATCH /projets-recrutement/{id}/fermer
     * Accès : ADMIN, DRH uniquement
     */
    @PatchMapping("/{id}/fermer")
    public ResponseEntity<ProjetRecrutementResponse> fermer(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.fermer(id));
    }
}
