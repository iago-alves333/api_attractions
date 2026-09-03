package br.ufpb.iago.backend.security;

import br.ufpb.iago.backend.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Testes unitarios para a classe JwtService.
 * Valida a geracao e estracao de informacoes do token JWT.
 */
@ExtendWith(SpringExtension.class)
public class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup(){
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "minha-chave-secreta-de-teste-com-256-bits-minimo!");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 86400000L);

    }
    @Test
    void mustGenarateValidToken(){
        // Gera token para usuario de teste
        String token = jwtService.generateToken("usuario-teste", Role.TOURIST);

        // Garante que o token nao e nulo e nao e vazio
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();

        // um jwt sempre tem 3 partes separados por ponto
        assertThat(token.split("\\.")).hasSize(3);
    }
    @Test
    void mustExtractUsernameFromTokenSuccessfully(){
        String originalUsername = "user@test.com";

        // Gera token de teste
        String token = jwtService.generateToken(originalUsername, Role.TOURIST);

        // Extrai o nome de usuário do token
        String extractedUsername = jwtService.extractEmailFromToken(token);

        // Garante que o nome de usuário extraído é igual ao original
        assertThat(extractedUsername).isEqualTo(originalUsername);
    }

    @Test
    void mustExtractRoleFromTokenSuccessfully(){
        String originalUsername = "user@test.com";
        Role originalRole = Role.TOURIST;

        // Gera token de teste
        String token = jwtService.generateToken(originalUsername, originalRole);

        // Extrai o papel do token
        String extractedRole = jwtService.extractRoleFromToken(token);

        // Garante que o papel extraído é igual ao original
        assertThat(extractedRole).isEqualTo(originalRole.toString());
    }

    @Test
    void mustValidateTokenCorrectly(){
        String token = jwtService.generateToken("admin@test.com", Role.ADMIN);
        boolean isValid = jwtService.isTokenValid(token);

        // Garante que o token é válido
        assertThat(isValid).isTrue();
    }}
