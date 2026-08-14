package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.ReservationRequestDTO;
import br.ufpb.iago.backend.dto.ReservationResponseDTO;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<ReservationResponseDTO>> findAll(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(reservationService.findAllByTourist(currentUser.getId()));
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
}
