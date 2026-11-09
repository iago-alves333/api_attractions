package br.ufpb.iago.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.UUID;

public record ReviewRequestDTO(
        @NotNull(message = "O ID da atração é obrigatório")
        UUID attractionId,

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 1, message = "A nota mínima é 1")
        @Max(value = 5, message = "A nota máxima é 5")
        Integer rating,

        @Size(max = 500, message = "O comentário não pode ter mais de 500 caracteres")
        String comment
) implements Serializable {}
