package br.ufpb.iago.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AttractionResponseDTO(
        UUID id,
        UUID guideId,
        String guideName,
        String title,
        String description,
        BigDecimal price,
        int availableSpots,
        double latitude,
        double longitude,
        double ratingAverage,
        int reviewCount
) {}