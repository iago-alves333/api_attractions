package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.UserRequestDTO;
import br.ufpb.iago.backend.dto.UserResponseDTO;
import br.ufpb.iago.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/v1/users
     * Cria um usuário diretamente (uso administrativo). Exige role ADMIN.
     */
    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO response = userService.saveUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/users
     * Lista todos os usuários. Exige role ADMIN.
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    /**
     * GET /api/v1/users/{id}
     * Busca um usuário pelo ID. Exige role ADMIN.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * DELETE /api/v1/users/{id}
     * Remove um usuário. Exige role ADMIN.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
