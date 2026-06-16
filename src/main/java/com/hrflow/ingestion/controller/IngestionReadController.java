package com.hrflow.ingestion.controller;

import com.hrflow.ingestion.dto.IngestionRecordResponse;
import com.hrflow.ingestion.model.IngestionSource;
import com.hrflow.ingestion.model.IngestionStatus;
import com.hrflow.ingestion.service.IngestionReadService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints en lecture pour la « Boîte de réception » DRH (frontend
 * {@code /inbox}).
 *
 * <h2>Pourquoi un controller séparé de {@link IngestController} ?</h2>
 *
 * <p>{@code IngestController} (write-side) est protégé par {@code X-Ingest-Key}
 * (filtre {@code IngestApiKeyFilter}) — c'est l'entrée technique pour n8n. Il
 * pose une authority synthétique {@code INGEST} qui n'a rien à voir avec un
 * utilisateur DRH connecté en JWT.
 *
 * <p>Ce controller-ci (read-side) suit la chaîne JWT classique : Bearer token
 * + {@code @PreAuthorize}. Mettre les 2 endpoints dans la même classe aurait
 * mélangé 2 schémas d'authentification incompatibles.
 *
 * <h2>Routes</h2>
 *
 * <ul>
 *   <li>{@code GET /ingestion/records} — liste paginée filtrable</li>
 *   <li>{@code GET /ingestion/records/{id}} — détail d'un record</li>
 * </ul>
 *
 * <p>Sécurité : accessible par DRH et ADMIN. Le DIRECTEUR n'a pas accès à la
 * vue d'audit globale — il consulte les candidatures finales via les pages
 * existantes (cohérent avec sa portée par-direction).
 */
@RestController
@RequestMapping("/ingestion/records")
public class IngestionReadController {

    private final IngestionReadService service;

    public IngestionReadController(IngestionReadService service) {
        this.service = service;
    }

    /**
     * Liste paginée et filtrée des records d'ingestion (tri receivedAt desc).
     *
     * @param status filtre optionnel — un statut précis (PENDING/IMPORTED/REJECTED/ERROR)
     * @param source filtre optionnel — un canal d'origine (EMAIL, MANUAL_UI)
     * @param page   numéro de page 0-indexée (défaut 0)
     * @param size   taille de page (défaut 20, plafond serveur 100)
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @GetMapping
    public ResponseEntity<Page<IngestionRecordResponse>> search(
            @RequestParam(required = false) IngestionStatus status,
            @RequestParam(required = false) IngestionSource source,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(status, source, page, size));
    }

    /**
     * Détail d'un record — utilisé par la page de détail {@code /inbox/:id}
     * du frontend.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DRH')")
    @GetMapping("/{id}")
    public ResponseEntity<IngestionRecordResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
