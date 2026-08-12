package br.ufpb.iago.backend.dto;

import br.ufpb.iago.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserRequestDTO {

    @NotBlank(message = "O nome não pode estar em branco")
    @Size(min=2, max=100, message = "O nome deve ter entre 2 e 100 caracteres")
    private String name;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "A senha não pode estar em branco")
    @Size(min=6, max=50, message = "A senha deve ter entre 6 e 50 caracteres")
    private String password;

    @NotNull(message = "O role é obrigatório")
    private Role role;

    // Getters e Setters continuam iguais...
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}