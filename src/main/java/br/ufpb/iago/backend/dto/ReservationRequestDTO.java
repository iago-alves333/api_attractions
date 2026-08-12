package br.ufpb.iago.backend.dto;

import br.ufpb.iago.backend.model.Status;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReservationRequestDTO {

    @NotNull(message = "O ID da atração é obrigatório")
    private UUID attractionId;

    @NotNull(message = "A hora da reserva é obrigatória")
    private LocalDateTime reservedFor;

    public UUID getAttractionId() {
        return attractionId;
    }

    public void setAttractionId(UUID attractionId) {
        this.attractionId = attractionId;
    }

    public LocalDateTime getReservedFor() {
        return reservedFor;
    }

    public void setReservedFor(LocalDateTime reservedFor) {
        this.reservedFor = reservedFor;
    }
}