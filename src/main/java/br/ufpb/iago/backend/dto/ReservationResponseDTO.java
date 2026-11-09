package br.ufpb.iago.backend.dto;

import br.ufpb.iago.backend.model.Status;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponseDTO(
        UUID id,
        UUID touristId,
        String touristName,
        UUID attractionId,
        String attractionTitle,
        Status status,
        LocalDateTime reservedFor,
        LocalDateTime createdAt
) implements Serializable {}
