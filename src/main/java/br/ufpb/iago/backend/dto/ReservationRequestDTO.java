package br.ufpb.iago.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationRequestDTO(
        @NotNull(message = "O ID da atração é obrigatório")
        UUID attractionId,

        @NotNull(message = "A hora da reserva é obrigatória")
        @Future(message = "A data da reserva deve ser no futuro")
        LocalDateTime reservedFor
) implements Serializable {}