package br.ufpb.iago.backend.dto;

import br.ufpb.iago.backend.model.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponseDTO (
        UUID id,
        UUID touristId,
        UUID attractionID,
        Status status,
        LocalDateTime reservedFor,
        LocalDateTime createdAt
){ }
