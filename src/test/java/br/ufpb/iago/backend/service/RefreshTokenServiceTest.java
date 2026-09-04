package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.exception.ExpiredRefreshTokenException;
import br.ufpb.iago.backend.exception.InvalidRefreshTokenException;
import br.ufpb.iago.backend.model.RefreshToken;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link RefreshTokenService}.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long REFRESH_EXPIRATION_MS = 604_800_000L; // 7 dias

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, REFRESH_EXPIRATION_MS);

        user = new User();
        // ajuste os setters conforme os campos reais da sua entidade User
        // user.setId(1L);
        // user.setEmail("teste@ufpb.br");
    }

    // ---------------------- createRefreshToken ----------------------

    @Test
    void createRefreshToken_deveGerarTokenValidoEPersistir() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getToken()).isNotBlank();
        assertThat(UUID.fromString(saved.getToken())).isNotNull(); // deve ser um UUID válido
        assertThat(result).isSameAs(saved);
    }

    @Test
    void createRefreshToken_deveDefinirExpiresAtConformeRefreshExpirationMs() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antes = LocalDateTime.now();
        RefreshToken result = refreshTokenService.createRefreshToken(user);
        LocalDateTime depois = LocalDateTime.now();

        LocalDateTime limiteInferior = antes.plusNanos(REFRESH_EXPIRATION_MS * 1_000_000L)
                .minus(1, ChronoUnit.SECONDS);
        LocalDateTime limiteSuperior = depois.plusNanos(REFRESH_EXPIRATION_MS * 1_000_000L)
                .plus(1, ChronoUnit.SECONDS);

        assertThat(result.getExpiresAt())
                .isAfterOrEqualTo(limiteInferior)
                .isBeforeOrEqualTo(limiteSuperior);
    }

    @Test
    void createRefreshToken_doisTokensSucessivosDevemSerDiferentes() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token1 = refreshTokenService.createRefreshToken(user);
        RefreshToken token2 = refreshTokenService.createRefreshToken(user);

        assertThat(token1.getToken()).isNotEqualTo(token2.getToken());
    }

    // ---------------------- validateRefreshToken ----------------------

    @Test
    void validateRefreshToken_tokenValidoENaoExpirado_deveRetornarRefreshToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("token-valido");
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(refreshTokenRepository.findByToken("token-valido"))
                .thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.validateRefreshToken("token-valido");

        assertThat(result).isEqualTo(refreshToken);
        verify(refreshTokenRepository).findByToken("token-valido");
    }

    @Test
    void validateRefreshToken_tokenInexistente_deveLancarInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken("token-inexistente"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("token-inexistente"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateRefreshToken_tokenExpirado_deveLancarExpiredRefreshTokenException() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("token-expirado");
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenRepository.findByToken("token-expirado"))
                .thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("token-expirado"))
                .isInstanceOf(ExpiredRefreshTokenException.class);
    }

    // ---------------------- deleteByToken ----------------------

    @Test
    void deleteByToken_deveDelegarParaORepository() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("token-a-remover");

        refreshTokenService.deleteByToken(refreshToken);

        verify(refreshTokenRepository, times(1)).delete(refreshToken);
        verifyNoMoreInteractions(refreshTokenRepository);
    }

    // ---------------------- deleteAllByUser ----------------------

    @Test
    void deleteAllByUser_deveDelegarParaORepository() {
        refreshTokenService.deleteAllByUser(user);

        verify(refreshTokenRepository, times(1)).deleteAllByUser(user);
        verifyNoMoreInteractions(refreshTokenRepository);
    }
}