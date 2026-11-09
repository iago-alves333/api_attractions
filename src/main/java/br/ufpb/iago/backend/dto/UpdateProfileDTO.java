package br.ufpb.iago.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record UpdateProfileDTO(
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
        String name,

        @Email(message = "Formato de email inválido")
        String email,

        @Size(min = 6, max = 50, message = "A senha deve ter entre 6 e 50 caracteres")
        String password
) implements Serializable {}
