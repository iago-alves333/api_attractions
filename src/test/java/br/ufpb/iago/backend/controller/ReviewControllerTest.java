package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.ReviewRequestDTO;
import br.ufpb.iago.backend.dto.ReviewResponseDTO;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private CustomUserDetails currentUser;
    private ReviewResponseDTO responseDTO;
    private UUID revId;
    private UUID attrId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.TOURIST);
        currentUser = new CustomUserDetails(user);

        revId = UUID.randomUUID();
        attrId = UUID.randomUUID();
        responseDTO = new ReviewResponseDTO(revId, attrId, currentUser.getId(), "Tourist", 5, "Comment", LocalDateTime.now());
    }

    @Test
    @DisplayName("Create deve retornar 201")
    void testCreate() {
        ReviewRequestDTO req = new ReviewRequestDTO();
        when(reviewService.create(req, currentUser.getId())).thenReturn(responseDTO);

        ResponseEntity<ReviewResponseDTO> response = reviewController.create(req, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }

    @Test
    @DisplayName("Find All deve retornar lista")
    void testFindAll() {
        Pageable pageable = Pageable.unpaged();
        Page<ReviewResponseDTO> page = new PageImpl<>(List.of(responseDTO));
        when(reviewService.findAll(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<ReviewResponseDTO>> response = reviewController.findAll(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Find By Attraction deve retornar lista")
    void testFindByAttraction() {
        Pageable pageable = Pageable.unpaged();
        Page<ReviewResponseDTO> page = new PageImpl<>(List.of(responseDTO));
        when(reviewService.findByAttraction(eq(attrId), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<ReviewResponseDTO>> response = reviewController.findByAttraction(attrId, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Find By Id deve retornar review")
    void testFindById() {
        when(reviewService.findById(revId)).thenReturn(responseDTO);

        ResponseEntity<ReviewResponseDTO> response = reviewController.findById(revId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Update deve retornar review atualizado")
    void testUpdate() {
        ReviewRequestDTO req = new ReviewRequestDTO();
        when(reviewService.update(revId, req, currentUser.getId())).thenReturn(responseDTO);

        ResponseEntity<ReviewResponseDTO> response = reviewController.update(revId, req, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Delete deve retornar 204")
    void testDelete() {
        doNothing().when(reviewService).delete(revId, currentUser.getId());

        ResponseEntity<Void> response = reviewController.delete(revId, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
