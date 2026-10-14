package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.*;
import br.ufpb.iago.backend.exception.TooManyRequestsException;
import br.ufpb.iago.backend.model.RefreshToken;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.UserRepository;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.security.HashUtils;
import br.ufpb.iago.backend.security.JwtService;
import br.ufpb.iago.backend.service.RefreshTokenService;
import br.ufpb.iago.backend.service.UserService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ProxyManager<String> proxyManager;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          UserRepository userRepository,
                          StringRedisTemplate stringRedisTemplate,
                          ProxyManager<String> proxyManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.proxyManager = proxyManager;
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
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto,
                                                  HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String email = dto.getEmail() != null ? dto.getEmail().toLowerCase().trim() : "";
        String rateLimitKey = "login_limit:" + clientIp + ":" + email;
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build())
                .build();

        ConsumptionProbe probe = proxyManager.builder().build(rateLimitKey, configSupplier).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new TooManyRequestsException("Muitas tentativas de login. Tente novamente em instantes.");
        }

        User user = userService.login(dto);

        String accessToken = jwtService.generateToken(user.getId(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new LoginResponseDTO(
                accessToken,
                refreshToken.getRawToken(),
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
        String newAccessToken = jwtService.generateToken(user.getId(), user.getRole());

        return ResponseEntity.ok(new RefreshResponseDTO(newAccessToken, newRefreshToken.getRawToken()));
    }

    /**
     * POST /api/v1/auth/logout
     * Insere o Access Token atual na blacklist Redis e revoga todos os Refresh Tokens do usuário.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       @AuthenticationPrincipal CustomUserDetails currentUser) {
        // Extrai o access token do header e insere na blacklist do Redis com TTL restante
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                LocalDateTime expiresAt = jwtService.extractExpirationFromToken(accessToken);
                long ttlSeconds = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
                if (ttlSeconds > 0) {
                    stringRedisTemplate.opsForValue().set(
                            "jwt:bl:" + HashUtils.sha256(accessToken),
                            "1",
                            Duration.ofSeconds(ttlSeconds)
                    );
                }
            } catch (Exception ignored) {
                // Token expirado ou inválido
            }
        }

        // Revoga todos os refresh tokens do usuário
        if (currentUser != null && currentUser.getId() != null) {
            User user = userRepository.findById(currentUser.getId()).orElse(null);
            if (user != null) {
                refreshTokenService.deleteAllByUser(user);
            }
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