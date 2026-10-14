package br.ufpb.iago.backend.security;

import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Requisição sem header Authorization deve apenas continuar a cadeia de filtros")
    void doFilterInternal_withoutAuthHeader_continuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Token na blacklist do Redis não deve autenticar o usuário")
    void doFilterInternal_blacklistedToken_doesNotAuthenticate() throws Exception {
        String token = "blacklisted-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(stringRedisTemplate.hasKey("jwt:bl:" + HashUtils.sha256(token))).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Token válido autentica o usuário pelo UUID no SecurityContext")
    void doFilterInternal_validToken_authenticatesUser() throws Exception {
        String token = "valid-token";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("tourist@test.com");
        user.setPassword("secret");
        user.setRole(Role.TOURIST);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(stringRedisTemplate.hasKey("jwt:bl:" + HashUtils.sha256(token))).thenReturn(false);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserIdFromToken(token)).thenReturn(userId);
        when(customUserDetailsService.loadUserById(userId)).thenReturn(new CustomUserDetails(user));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("tourist@test.com");
    }
}
