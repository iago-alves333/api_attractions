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

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Cria um novo refresh token para o usuário e salva no banco.
     * O token é um UUID aleatório (opaco), não um JWT.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L));

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Valida o refresh token: verifica se existe no banco e se não expirou.
     * Retorna o RefreshToken (com o User associado) se válido.
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
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
}
