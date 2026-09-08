package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.*;
import br.ufpb.iago.backend.model.RefreshToken;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.TokenBlacklistRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.security.JwtService;
import br.ufpb.iago.backend.service.RefreshTokenService;
import br.ufpb.iago.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    private User validUser;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        validUser = new User();
        validUser.setId(UUID.randomUUID());
        validUser.setName("Test User");
        validUser.setEmail("test@gmail.com");
        validUser.setPassword("encoded-password123");
        validUser.setRole(Role.GUIDE);

        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@gmail.com");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("Login com credenciais válidas deve retornar 200 e um corpo com o token")
    void testLoginWithValidCredentials_ReturnsOkWithToken() {
        when(userService.login(any())).thenReturn(validUser);
        when(jwtService.generateToken(validUser.getEmail(), validUser.getRole())).thenReturn("mocked-jwt-token");
        RefreshToken rt = new RefreshToken();
        rt.setToken("mocked-refresh-token");
        when(refreshTokenService.createRefreshToken(validUser)).thenReturn(rt);

        ResponseEntity<LoginResponseDTO> response = authController.login(loginRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("mocked-refresh-token");
        
        verify(userService, times(1)).login(loginRequest);
    }
    
    @Test
    @DisplayName("Register com dados válidos deve retornar 201 e dados do usuário")
    void testRegister_ReturnsCreated() {
        UserRequestDTO req = new UserRequestDTO();
        UserResponseDTO resp = new UserResponseDTO(UUID.randomUUID(), "Test", Role.TOURIST, null);
        
        when(userService.saveUser(req)).thenReturn(resp);
        
        ResponseEntity<UserResponseDTO> response = authController.register(req);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("Refresh deve retornar novo token e novo refresh token")
    void testRefresh_ReturnsOk() {
        RefreshRequestDTO req = new RefreshRequestDTO();
        req.setRefreshToken("old-refresh");
        
        RefreshToken oldRt = new RefreshToken();
        oldRt.setUser(validUser);
        oldRt.setToken("old-refresh");
        
        RefreshToken newRt = new RefreshToken();
        newRt.setToken("new-refresh");

        when(refreshTokenService.validateRefreshToken("old-refresh")).thenReturn(oldRt);
        when(refreshTokenService.createRefreshToken(validUser)).thenReturn(newRt);
        when(jwtService.generateToken(validUser.getEmail(), validUser.getRole())).thenReturn("new-access");

        ResponseEntity<RefreshResponseDTO> response = authController.refresh(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("new-access");
        assertThat(response.getBody().refreshToken()).isEqualTo("new-refresh");
        
        verify(refreshTokenService).deleteByToken(oldRt);
    }

    @Test
    @DisplayName("Logout deve invalidar o token e revogar refresh tokens")
    void testLogout() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer my-token");
        
        CustomUserDetails userDetails = new CustomUserDetails(validUser);
        when(userRepository.findById(validUser.getId())).thenReturn(Optional.of(validUser));
        
        ResponseEntity<Void> response = authController.logout(request, userDetails);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        verify(tokenBlacklistRepository).save(any());
        verify(refreshTokenService).deleteAllByUser(validUser);
    }

    @Test
    @DisplayName("Get Me deve retornar dados do usuário atual")
    void testGetMe() {
        CustomUserDetails userDetails = new CustomUserDetails(validUser);
        UserResponseDTO resp = new UserResponseDTO(validUser.getId(), "Test User", Role.GUIDE, null);
        
        when(userService.findById(validUser.getId())).thenReturn(resp);
        
        ResponseEntity<UserResponseDTO> response = authController.getCurrentUser(userDetails);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("Update Profile deve retornar dados atualizados do usuário")
    void testUpdateProfile() {
        CustomUserDetails userDetails = new CustomUserDetails(validUser);
        UpdateProfileDTO req = new UpdateProfileDTO();
        UserResponseDTO resp = new UserResponseDTO(validUser.getId(), "New Name", Role.GUIDE, null);
        
        when(userService.updateProfile(validUser.getId(), req)).thenReturn(resp);
        
        ResponseEntity<UserResponseDTO> response = authController.updateProfile(req, userDetails);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }
}