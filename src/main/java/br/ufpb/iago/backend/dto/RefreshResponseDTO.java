package br.ufpb.iago.backend.dto;

public record RefreshResponseDTO(
        String accessToken,
        String refreshToken
) {}
