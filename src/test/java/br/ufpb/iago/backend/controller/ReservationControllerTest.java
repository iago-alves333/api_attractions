package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.ReservationRequestDTO;
import br.ufpb.iago.backend.dto.ReservationResponseDTO;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.model.Status;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController reservationController;

    private CustomUserDetails currentUser;
    private ReservationResponseDTO responseDTO;
    private UUID resId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.TOURIST);
        currentUser = new CustomUserDetails(user);
        
        resId = UUID.randomUUID();
        responseDTO = new ReservationResponseDTO(resId, UUID.randomUUID(), UUID.randomUUID(), Status.PENDING, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Create deve retornar 201")
    void testCreate() {
        ReservationRequestDTO req = new ReservationRequestDTO();
        when(reservationService.create(req, currentUser.getId())).thenReturn(responseDTO);

        ResponseEntity<ReservationResponseDTO> response = reservationController.create(req, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }

    @Test
    @DisplayName("Find All deve retornar lista")
    void testFindAll() {
        when(reservationService.findAllByTourist(currentUser.getId())).thenReturn(List.of(responseDTO));

        ResponseEntity<List<ReservationResponseDTO>> response = reservationController.findAll(currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Find By Id deve retornar reserva")
    void testFindById() {
        when(reservationService.findById(resId, currentUser.getId())).thenReturn(responseDTO);

        ResponseEntity<ReservationResponseDTO> response = reservationController.findById(resId, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }

    @Test
    @DisplayName("Cancel deve retornar reserva cancelada")
    void testCancel() {
        when(reservationService.cancel(resId, currentUser.getId())).thenReturn(responseDTO);

        ResponseEntity<ReservationResponseDTO> response = reservationController.cancel(resId, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Confirm deve retornar reserva confirmada")
    void testConfirm() {
        when(reservationService.confirm(resId, currentUser.getId())).thenReturn(responseDTO);

        ResponseEntity<ReservationResponseDTO> response = reservationController.confirm(resId, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Complete deve retornar reserva concluida")
    void testComplete() {
        when(reservationService.complete(resId, currentUser.getId())).thenReturn(responseDTO);

        ResponseEntity<ReservationResponseDTO> response = reservationController.complete(resId, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Find All By Guide deve retornar lista")
    void testFindAllByGuide() {
        when(reservationService.findAllByGuide(currentUser.getId())).thenReturn(List.of(responseDTO));

        ResponseEntity<List<ReservationResponseDTO>> response = reservationController.findAllByGuide(currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
