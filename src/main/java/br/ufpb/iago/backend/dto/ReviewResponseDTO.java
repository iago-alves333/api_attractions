package br.ufpb.iago.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponseDTO(
        UUID id,
        UUID attractionId,
        UUID touristId,
        String touristName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {}