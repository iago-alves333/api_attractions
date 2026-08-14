package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.LoginRequestDTO;
import br.ufpb.iago.backend.dto.LoginResponseDTO;
import br.ufpb.iago.backend.dto.UserRequestDTO;
import br.ufpb.iago.backend.dto.UserResponseDTO;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.security.JwtService;
import br.ufpb.iago.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/v1/auth/register
     * Registra um novo usuário (TOURIST ou GUIDE) e retorna seus dados.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.saveUser(dto));
    }

    /**
     * POST /api/v1/auth/login
     * Autentica o usuário com email/senha e retorna um JWT.
     * A validação de credenciais é delegada ao UserService.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        User user = userService.login(dto);

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                user.getId(),
                user.getName(),
                user.getRole()
        ));
    }
}