package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.AttractionRequestDTO;
import br.ufpb.iago.backend.dto.AttractionResponseDTO;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.service.AttractionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attractions")
public class AttractionController {

    private final AttractionService attractionService;

    public AttractionController(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    /**
     * POST /api/v1/attractions
     * Cria uma nova atração. Exige role GUIDE (configurado no SecurityConfig).
     */
    @PostMapping
    public ResponseEntity<AttractionResponseDTO> create(
            @Valid @RequestBody AttractionRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        AttractionResponseDTO response = attractionService.create(dto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/attractions
     * Lista todas as atrações. Acesso público.
     */
    @GetMapping
    public ResponseEntity<List<AttractionResponseDTO>> findAll() {
        return ResponseEntity.ok(attractionService.findAll());
    }

    /**
     * GET /api/v1/attractions/{id}
     * Busca uma atração pelo ID. Acesso público.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AttractionResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(attractionService.findById(id));
    }

    /**
     * PUT /api/v1/attractions/{id}
     * Atualiza uma atração. Exige role GUIDE e ser o dono da atração.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AttractionResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody AttractionRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        AttractionResponseDTO response = attractionService.update(id, dto, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/attractions/{id}
     * Remove uma atração. Exige role GUIDE e ser o dono da atração.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        attractionService.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
