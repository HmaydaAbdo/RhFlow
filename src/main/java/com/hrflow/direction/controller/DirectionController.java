package com.hrflow.direction.controller;

import com.hrflow.direction.service.DirectionService;
import com.hrflow.direction.dto.DirectionRequest;
import com.hrflow.direction.dto.DirectionResponse;
import com.hrflow.direction.dto.DirectionSearchDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/directions")
public class DirectionController {

    private final DirectionService directionService;

    public DirectionController(DirectionService directionService) {
        this.directionService = directionService;
    }

    @GetMapping
    public ResponseEntity<Page<DirectionResponse>> search(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) Long directeurId,
            @PageableDefault(size = 20, sort = "nom") Pageable pageable) {

        return ResponseEntity.ok(
                directionService.search(new DirectionSearchDto(nom, directeurId), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DirectionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(directionService.findById(id));
    }

    @PostMapping
    public ResponseEntity<DirectionResponse> create(@Valid @RequestBody DirectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(directionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DirectionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DirectionRequest request) {
        return ResponseEntity.ok(directionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        directionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
