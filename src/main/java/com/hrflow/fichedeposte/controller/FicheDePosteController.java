package com.hrflow.fichedeposte.controller;

import com.hrflow.fichedeposte.service.FicheDePosteService;
import com.hrflow.fichedeposte.dto.FicheDePosteRequest;
import com.hrflow.fichedeposte.dto.FicheDePosteResponse;
import com.hrflow.fichedeposte.dto.FicheDePosteSummaryResponse;
import com.hrflow.fichedeposte.dto.FicheDePosteSearchDto;
import com.hrflow.fichedeposte.model.NiveauEtudes;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fiches-de-poste")
public class FicheDePosteController {

    private final FicheDePosteService ficheDePosteService;

    public FicheDePosteController(FicheDePosteService ficheDePosteService) {
        this.ficheDePosteService = ficheDePosteService;
    }

    @GetMapping
    public ResponseEntity<Page<FicheDePosteSummaryResponse>> search(
            @RequestParam(required = false) String intitulePoste,
            @RequestParam(required = false) Long directionId,
            @RequestParam(required = false) NiveauEtudes niveauEtudes,
            @PageableDefault(size = 20, sort = "intitulePoste") Pageable pageable) {

        FicheDePosteSearchDto search = new FicheDePosteSearchDto(intitulePoste, directionId, niveauEtudes);
        return ResponseEntity.ok(ficheDePosteService.search(search, pageable));
    }

    @GetMapping("/par-direction/{directionId}")
    public ResponseEntity<Page<FicheDePosteSummaryResponse>> findByDirection(
            @PathVariable Long directionId,
            @PageableDefault(size = 20, sort = "intitulePoste") Pageable pageable) {

        return ResponseEntity.ok(ficheDePosteService.findByDirection(directionId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FicheDePosteResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ficheDePosteService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    public ResponseEntity<FicheDePosteResponse> create(@Valid @RequestBody FicheDePosteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ficheDePosteService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    public ResponseEntity<FicheDePosteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FicheDePosteRequest request) {
        return ResponseEntity.ok(ficheDePosteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ficheDePosteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
