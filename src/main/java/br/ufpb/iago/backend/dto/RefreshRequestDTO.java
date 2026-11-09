package br.ufpb.iago.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record RefreshRequestDTO(
        @NotBlank(message = "O refresh token é obrigatório")
        String refreshToken
) implements Serializable {}
