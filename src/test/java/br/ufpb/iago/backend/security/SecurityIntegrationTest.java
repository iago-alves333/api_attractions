package br.ufpb.iago.backend.security;

import br.ufpb.iago.backend.config.CorsConfig;
import br.ufpb.iago.backend.config.SecurityConfig;
import br.ufpb.iago.backend.controller.AttractionController;
import br.ufpb.iago.backend.controller.ReservationController;
import br.ufpb.iago.backend.controller.UserController;
import br.ufpb.iago.backend.controller.WebhookController;
import br.ufpb.iago.backend.dto.PageDTO;
import br.ufpb.iago.backend.dto.ReservationResponseDTO;
import br.ufpb.iago.backend.exception.GlobalExceptionHandler;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.Status;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.UserRepository;
import br.ufpb.iago.backend.service.AttractionService;
import br.ufpb.iago.backend.service.ReservationService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ReservationController.class, AttractionController.class, UserController.class, WebhookController.class})
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
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ProxyManager<String> proxyManager;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private AttractionService attractionService;

    @MockBean
    private UserService userService;

    private User tourist;
    private User guide;
    private User admin;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RemoteBucketBuilder<String> bucketBuilder = mock(RemoteBucketBuilder.class);
        BucketProxy bucketProxy = mock(BucketProxy.class);
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(any(String.class), any(Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.consumed(99, 0));
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        tourist = new User();
        tourist.setId(UUID.randomUUID());
        tourist.setEmail("tourist@titureco.com");
        tourist.setPassword("hashed");
        tourist.setRole(Role.TOURIST);

        guide = new User();
        guide.setId(UUID.randomUUID());
        guide.setEmail("guide@titureco.com");
        guide.setPassword("hashed");
        guide.setRole(Role.GUIDE);

        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@titureco.com");
        admin.setPassword("hashed");
        admin.setRole(Role.ADMIN);

        when(userRepository.findById(tourist.getId())).thenReturn(Optional.of(tourist));
        when(userRepository.findById(guide.getId())).thenReturn(Optional.of(guide));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    }

    @Test
    @DisplayName("GET /api/v1/attractions deve ser público sem autenticação")
    void publicAttractionListingWorksWithoutAuth() throws Exception {
        when(attractionService.findAll(any(Pageable.class)))
                .thenReturn(new PageDTO<>(Page.empty()));

        mockMvc.perform(get("/api/v1/attractions"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Rate-Limit-Remaining"));
    }

    @Test
    @DisplayName("GET /api/v1/reservations/guide/attractions/{id} deve permitir GUIDE e bloquear TOURIST (regressão 🔴2)")
    void guideParticipantsRouteAllowsGuideAndBlocksTourist() throws Exception {
        UUID attractionId = UUID.randomUUID();
        when(reservationService.findAllByAttraction(eq(attractionId), eq(guide.getId()), anyBoolean(), any(Pageable.class)))
                .thenReturn(Page.empty());

        String guideToken = jwtService.generateToken(guide.getId(), Role.GUIDE);
        String touristToken = jwtService.generateToken(tourist.getId(), Role.TOURIST);

        mockMvc.perform(get("/api/v1/reservations/guide/attractions/" + attractionId)
                        .header("Authorization", "Bearer " + guideToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reservations/guide/attractions/" + attractionId)
                        .header("Authorization", "Bearer " + touristToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Token na blacklist do Redis deve ser rejeitado em rota protegida")
    void blacklistedJwtTokenIsRejected() throws Exception {
        String token = jwtService.generateToken(tourist.getId(), Role.TOURIST);
        when(stringRedisTemplate.hasKey("jwt:bl:" + HashUtils.sha256(token))).thenReturn(true);

        mockMvc.perform(get("/api/v1/reservations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN deve conseguir deletar atração via DELETE /api/v1/attractions/{id}")
    void adminCanDeleteAttraction() throws Exception {
        UUID attractionId = UUID.randomUUID();
        String adminToken = jwtService.generateToken(admin.getId(), Role.ADMIN);

        mockMvc.perform(delete("/api/v1/attractions/" + attractionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/reservations/{id}/confirm deve permitir chamada M2M sem JWT")
    void webhookConfirmAllowsPostWithoutJwt() throws Exception {
        UUID reservationId = UUID.randomUUID();
        ReservationResponseDTO dto = new ReservationResponseDTO(
                reservationId, tourist.getId(), "Tourist", UUID.randomUUID(), "Attraction",
                Status.CONFIRMED, LocalDateTime.now().plusDays(1), LocalDateTime.now()
        );
        when(reservationService.confirm(reservationId, null, true)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/webhooks/reservations/" + reservationId + "/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"payment.approved\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/webhooks/reservations/" + reservationId + "/confirm"))
                .andExpect(status().isForbidden());
    }
}
