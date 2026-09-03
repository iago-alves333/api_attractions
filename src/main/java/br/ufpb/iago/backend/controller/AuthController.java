package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.*;
import br.ufpb.iago.backend.model.RefreshToken;
import br.ufpb.iago.backend.model.TokenBlacklist;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.TokenBlacklistRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.security.JwtService;
import br.ufpb.iago.backend.service.RefreshTokenService;
import br.ufpb.iago.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final UserRepository userRepository;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          TokenBlacklistRepository tokenBlacklistRepository,
                          UserRepository userRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.userRepository = userRepository;
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
     * Autentica o usuário com email/senha e retorna um Access Token (curta duração)
     * e um Refresh Token (longa duração).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        User user = userService.login(dto);

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new LoginResponseDTO(
                accessToken,
                refreshToken.getToken(),
                user.getId(),
                user.getName(),
                user.getRole()
        ));
    }

    /**
     * POST /api/v1/auth/refresh
     * Valida o Refresh Token e emite um novo Access Token + novo Refresh Token (rotação).
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        // Valida o refresh token existente
        RefreshToken oldRefreshToken = refreshTokenService.validateRefreshToken(dto.getRefreshToken());
        User user = oldRefreshToken.getUser();

        // Rotação: remove o token antigo e cria um novo
        refreshTokenService.deleteByToken(oldRefreshToken);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        // Gera novo access token
        String newAccessToken = jwtService.generateToken(user.getEmail(), user.getRole());

        return ResponseEntity.ok(new RefreshResponseDTO(newAccessToken, newRefreshToken.getToken()));
    }

    /**
     * POST /api/v1/auth/logout
     * Insere o Access Token atual na blacklist e revoga todos os Refresh Tokens do usuário.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       @AuthenticationPrincipal CustomUserDetails currentUser) {
        // Extrai o access token do header e insere na blacklist
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);

            TokenBlacklist blacklistEntry = new TokenBlacklist();
            blacklistEntry.setToken(accessToken);
            blacklistEntry.setExpiresAt(jwtService.extractExpirationFromToken(accessToken));
            tokenBlacklistRepository.save(blacklistEntry);
        }

        // Revoga todos os refresh tokens do usuário
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user != null) {
            refreshTokenService.deleteAllByUser(user);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/auth/me
     * Retorna os dados do usuário autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UserResponseDTO user = userService.findById(currentUser.getId());
        return ResponseEntity.ok(user);
    }

    /**
     * PUT /api/v1/auth/me
     * Atualiza o perfil do usuário autenticado (nome, email, senha).
     * Apenas campos preenchidos serão atualizados.
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @Valid @RequestBody UpdateProfileDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        UserResponseDTO response = userService.updateProfile(currentUser.getId(), dto);
        return ResponseEntity.ok(response);
    }
}