package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.exception.ExpiredRefreshTokenException;
import br.ufpb.iago.backend.exception.InvalidRefreshTokenException;
import br.ufpb.iago.backend.model.RefreshToken;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Cria um novo refresh token para o usuário e salva no banco usando hash SHA-256.
     * O token bruto é retornado na propriedade rawToken da entidade.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String raw = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(br.ufpb.iago.backend.security.HashUtils.sha256(raw));
        refreshToken.setRawToken(raw);
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L));
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        saved.setRawToken(raw);
        return saved;
    }

    /**
     * Valida o refresh token: verifica se o hash existe no banco e se não expirou.
     * Retorna o RefreshToken (com o User associado) se válido.
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        String tokenHash = br.ufpb.iago.backend.security.HashUtils.sha256(rawToken);
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ExpiredRefreshTokenException();
        }
        return refreshToken;
    }

    /**
     * Remove um refresh token específico do banco (rotação).
     */
    @Transactional
    public void deleteByToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    /**
     * Remove todos os refresh tokens de um usuário (logout completo).
     */
    @Transactional
    public void deleteAllByUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    /**
     * Limpa periodicamente refresh tokens expirados do banco.
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}