package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.PageDTO;
import br.ufpb.iago.backend.dto.ReservationRequestDTO;
import br.ufpb.iago.backend.dto.ReservationResponseDTO;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * POST /api/v1/reservations
     * Cria uma nova reserva para o turista autenticado.
     */
    @PostMapping
    public ResponseEntity<ReservationResponseDTO> create(
            @Valid @RequestBody ReservationRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ReservationResponseDTO response = reservationService.create(dto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/reservations
     * Lista todas as reservas do turista autenticado.
     */
    @GetMapping
    public ResponseEntity<PageDTO<ReservationResponseDTO>> findAll(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(new PageDTO<>(reservationService.findAllByTourist(currentUser.getId(), pageable)));
    }

    /**
     * GET /api/v1/reservations/{id}
     * Busca uma reserva específica do turista autenticado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDTO> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(reservationService.findById(id, currentUser.getId()));
    }

    /**
     * PATCH /api/v1/reservations/{id}/cancel
     * Cancela uma reserva do turista autenticado.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponseDTO> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ReservationResponseDTO response = reservationService.cancel(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponseDTO> confirm(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ReservationResponseDTO response = reservationService.confirm(id, currentUser.getId(), currentUser.getRole() == Role.ADMIN);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/v1/reservations/{id}/complete
     * Marca uma reserva como concluída. Exige role GUIDE ou ADMIN e ser o dono da atração (ou ADMIN).
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ReservationResponseDTO> complete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ReservationResponseDTO response = reservationService.complete(id, currentUser.getId(), currentUser.getRole() == Role.ADMIN);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/reservations/guide
     * Lista todas as reservas das atrações do guia autenticado.
     */
    @GetMapping("/guide")
    public ResponseEntity<PageDTO<ReservationResponseDTO>> findAllByGuide(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(new PageDTO<>(reservationService.findAllByGuide(currentUser.getId(), pageable)));
    }

    /**
     * GET /api/v1/reservations/guide/attractions/{attractionId}
     * Lista todas as reservas de uma atração específica do guia autenticado.
     */
    @GetMapping("/guide/attractions/{attractionId}")
    public ResponseEntity<PageDTO<ReservationResponseDTO>> findAllByAttraction(
            @PathVariable UUID attractionId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(new PageDTO<>(reservationService.findAllByAttraction(attractionId, currentUser.getId(), currentUser.getRole() == Role.ADMIN, pageable)));
    }
}
