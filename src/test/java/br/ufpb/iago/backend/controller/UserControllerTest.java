package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.UserRequestDTO;
import br.ufpb.iago.backend.dto.UserResponseDTO;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserResponseDTO userResponse;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userResponse = new UserResponseDTO(userId, "Admin", Role.ADMIN, null);
    }

    @Test
    @DisplayName("Create User deve retornar 201 e dados")
    void testCreate() {
        UserRequestDTO req = new UserRequestDTO();
        when(userService.saveUser(req)).thenReturn(userResponse);

        ResponseEntity<UserResponseDTO> response = userController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(userResponse);
    }

    @Test
    @DisplayName("Find All deve retornar lista de usuários")
    void testFindAll() {
        when(userService.findAll()).thenReturn(List.of(userResponse));

        ResponseEntity<List<UserResponseDTO>> response = userController.findAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Find By Id deve retornar usuário")
    void testFindById() {
        when(userService.findById(userId)).thenReturn(userResponse);

        ResponseEntity<UserResponseDTO> response = userController.findById(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userResponse);
    }

    @Test
    @DisplayName("Delete deve retornar 204")
    void testDelete() {
        doNothing().when(userService).delete(userId);

        ResponseEntity<Void> response = userController.delete(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).delete(userId);
    }

    @Test
    @DisplayName("Promote To Guide deve retornar usuário promovido")
    void testPromoteToGuide() {
        when(userService.promoteToGuide(userId)).thenReturn(userResponse);

        ResponseEntity<UserResponseDTO> response = userController.promoteToGuide(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userResponse);
    }
}
