package br.ufpb.iago.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

public record AttractionRequestDTO(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título não pode ter mais de 150 caracteres")
        String title,

        @Size(max = 2000, message = "A descrição não pode ter mais de 2000 caracteres")
        String description,

        @NotNull(message = "O preço é obrigatório")
        @PositiveOrZero(message = "O preço não pode ser negativo")
        BigDecimal price,

        @NotNull(message = "A quantidade de vagas é obrigatória")
        @PositiveOrZero(message = "A quantidade de vagas não pode ser negativa")
        Integer availableSpots,

        @NotNull(message = "A latitude é obrigatória")
        @DecimalMin(value = "-90.0", message = "Latitude inválida")
        @DecimalMax(value = "90.0", message = "Latitude inválida")
        Double latitude,

        @NotNull(message = "A longitude é obrigatória")
        @DecimalMin(value = "-180.0", message = "Longitude inválida")
        @DecimalMax(value = "180.0", message = "Longitude inválida")
        Double longitude
) implements Serializable {}