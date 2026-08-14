package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.ReviewRequestDTO;
import br.ufpb.iago.backend.dto.ReviewResponseDTO;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * POST /api/v1/reviews
     * Cria um review para uma atração. Exige role TOURIST.
     */
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> create(
            @Valid @RequestBody ReviewRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ReviewResponseDTO response = reviewService.create(dto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/reviews
     * Lista todos os reviews. Acesso público.
     */
    @GetMapping
    public ResponseEntity<List<ReviewResponseDTO>> findAll() {
        return ResponseEntity.ok(reviewService.findAll());
    }

    /**
     * GET /api/v1/reviews/attraction/{attractionId}
     * Lista todos os reviews de uma atração específica. Acesso público.
     */
    @GetMapping("/attraction/{attractionId}")
    public ResponseEntity<List<ReviewResponseDTO>> findByAttraction(@PathVariable UUID attractionId) {
        return ResponseEntity.ok(reviewService.findByAttraction(attractionId));
    }

    /**
     * GET /api/v1/reviews/{id}
     * Busca um review pelo ID. Acesso público.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.findById(id));
    }

    /**
     * PUT /api/v1/reviews/{id}
     * Atualiza um review. Exige role TOURIST e ser o autor do review.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ReviewResponseDTO response = reviewService.update(id, dto, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/reviews/{id}
     * Deleta um review. Exige role TOURIST e ser o autor do review.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        reviewService.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
