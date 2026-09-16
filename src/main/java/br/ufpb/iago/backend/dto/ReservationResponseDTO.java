package br.ufpb.iago.backend.dto;

import br.ufpb.iago.backend.model.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponseDTO (
        UUID id,
        UUID touristId,
        String touristName,
        UUID attractionID,
        String attractionTitle,
        Status status,
        LocalDateTime reservedFor,
        LocalDateTime createdAt
) implements java.io.Serializable { }
