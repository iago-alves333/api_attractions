package br.ufpb.iago.backend.dto;

import br.ufpb.iago.backend.model.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDTO (
    UUID id,
    String name,
    Role role,
    LocalDateTime createdAt
) {}
