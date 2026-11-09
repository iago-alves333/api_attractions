package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.config.CorsConfig;
import br.ufpb.iago.backend.config.SecurityConfig;
import br.ufpb.iago.backend.dto.UserResponseDTO;
import br.ufpb.iago.backend.exception.BusinessException;
import br.ufpb.iago.backend.exception.GlobalExceptionHandler;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.UserRepository;
import br.ufpb.iago.backend.security.CustomUserDetailsService;
import br.ufpb.iago.backend.security.JwtAuthenticationFilter;
import br.ufpb.iago.backend.security.JwtService;
import br.ufpb.iago.backend.security.RateLimitFilter;
import br.ufpb.iago.backend.service.UserService;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        JwtAuthenticationFilter.class,
        RateLimitFilter.class,
        JwtService.class,
        CustomUserDetailsService.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-with-at-least-32-bytes-for-hmac-sha256",
        "jwt.expiration-ms=900000"
})
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ProxyManager<String> proxyManager;

    private User admin;
    private User tourist;
    private String adminToken;
    private String touristToken;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RemoteBucketBuilder<String> bucketBuilder = mock(RemoteBucketBuilder.class);
        BucketProxy bucketProxy = mock(BucketProxy.class);
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(any(String.class), any(Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.consumed(99, 0));
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@titureco.com");
        admin.setPassword("hashed");
        admin.setRole(Role.ADMIN);

        tourist = new User();
        tourist.setId(UUID.randomUUID());
        tourist.setEmail("tourist@titureco.com");
        tourist.setPassword("hashed");
        tourist.setRole(Role.TOURIST);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(tourist.getId())).thenReturn(Optional.of(tourist));

        adminToken = jwtService.generateToken(admin.getId(), Role.ADMIN);
        touristToken = jwtService.generateToken(tourist.getId(), Role.TOURIST);
    }

    @Test
    @DisplayName("GET /api/v1/users deve retornar PageDTO para ADMIN e 403 para TOURIST")
    void findAllUsersEnforcesAdminRoleAndReturnsPageDto() throws Exception {
        UserResponseDTO dto = new UserResponseDTO(tourist.getId(), "Turista", Role.TOURIST, java.time.LocalDateTime.now());

        when(userService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Turista"))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + touristToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/promote-guide deve promover turista quando chamado por ADMIN")
    void promoteToGuideAsAdminReturns200() throws Exception {
        UserResponseDTO promoted = new UserResponseDTO(tourist.getId(), tourist.getEmail(), Role.GUIDE, java.time.LocalDateTime.now());

        when(userService.promoteToGuide(tourist.getId())).thenReturn(promoted);

        mockMvc.perform(patch("/api/v1/users/" + tourist.getId() + "/promote-guide")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GUIDE"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} com registros vinculados deve retornar 409 Conflict")
    void deleteUserWithLinkedRecordsReturns409() throws Exception {
        doThrow(new BusinessException("Não é possível excluir um usuário que possui atrações, reservas ou avaliações vinculadas."))
                .when(userService).delete(tourist.getId());

        mockMvc.perform(delete("/api/v1/users/" + tourist.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/v1/users com payload inválido deve retornar 400 Bad Request com fieldErrors")
    void createUserWithInvalidPayloadReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalido\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}
