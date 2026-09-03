package br.ufpb.iago.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateProfileDTO {

    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    private String name;

    @Email(message = "Formato de email inválido")
    private String email;

    @Size(min = 6, max = 50, message = "A senha deve ter entre 6 e 50 caracteres")
    private String password;

    // Getters e Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
