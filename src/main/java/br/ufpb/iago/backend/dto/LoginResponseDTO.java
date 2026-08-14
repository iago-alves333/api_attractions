package br.ufpb.iago.backend.dto;

import br.ufpb.iago.backend.model.Role;

import java.util.UUID;

public record LoginResponseDTO(
        String token,
        UUID userId,
        String name,
        Role role
) {}
