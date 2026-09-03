package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.AttractionRequestDTO;
import br.ufpb.iago.backend.dto.AttractionResponseDTO;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.service.AttractionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    // ─── ENDPOINTS DE CRIAÇÃO E EDIÇÃO (PROTEGIDOS) ───────────────────────────

    @PostMapping
    public ResponseEntity<AttractionResponseDTO> create(
            @Valid @RequestBody AttractionRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        AttractionResponseDTO response = attractionService.create(dto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttractionResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody AttractionRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        AttractionResponseDTO response = attractionService.update(id, dto, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        attractionService.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    // ─── ENDPOINTS DE BUSCA E LISTAGEM (PÚBLICOS) ─────────────────────────────

    @GetMapping
    public ResponseEntity<Page<AttractionResponseDTO>> findAll(
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(attractionService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttractionResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(attractionService.findById(id));
    }

    /**
     * GET /api/v1/attractions/nearby?lat=X&lon=Y&radiusKm=Z
     * Busca atrações baseadas em um raio de distância usando PostGIS.
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<AttractionResponseDTO>> getNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10.0") double radiusKm
    ) {
        return ResponseEntity.ok(attractionService.getNearbyAttractions(lat, lon, radiusKm));
    }

    /**
     * GET /api/v1/attractions/search?keyword=X&lat=Y&lon=Z&radiusKm=W
     * Busca combinada por título/descrição e raio de distância.
     */
    @GetMapping("/search")
    public ResponseEntity<List<AttractionResponseDTO>> search(
            @RequestParam String keyword,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "50.0") double radiusKm
    ) {
        return ResponseEntity.ok(attractionService.searchAttractions(keyword, lat, lon, radiusKm));
    }
}