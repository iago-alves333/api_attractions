package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.ReservationResponseDTO;
import br.ufpb.iago.backend.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final ReservationService reservationService;

    public WebhookController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * POST /api/v1/webhooks/reservations/{id}/confirm
     * Endpoint M2M para confirmação automática de reserva (ex.: callback de pagamento).
     * A autenticação HMAC-SHA256 é aplicada na borda pelo API Gateway em rede interna.
     */
    @PostMapping("/reservations/{id}/confirm")
    public ResponseEntity<ReservationResponseDTO> confirmReservation(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.confirm(id, null, true));
    }
}
