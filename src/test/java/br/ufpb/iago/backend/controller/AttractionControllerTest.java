package br.ufpb.iago.backend.controller;

import br.ufpb.iago.backend.dto.AttractionRequestDTO;
import br.ufpb.iago.backend.dto.AttractionResponseDTO;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.security.CustomUserDetails;
import br.ufpb.iago.backend.service.AttractionService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttractionControllerTest {

    @Mock
    private AttractionService attractionService;

    @InjectMocks
    private AttractionController attractionController;

    private CustomUserDetails currentUser;
    private AttractionResponseDTO responseDTO;
    private UUID attrId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.GUIDE);
        currentUser = new CustomUserDetails(user);

        attrId = UUID.randomUUID();
        responseDTO = new AttractionResponseDTO(attrId, UUID.randomUUID(), "Guide Name", "Attr", "Desc", BigDecimal.valueOf(10.0), 10, 10.0, 10.0, 4.5, 2);
    }

    @Test
    @DisplayName("Create deve retornar 201 e dados")
    void testCreate() {
        AttractionRequestDTO req = new AttractionRequestDTO();
        when(attractionService.create(req, currentUser.getId())).thenReturn(responseDTO);

        ResponseEntity<AttractionResponseDTO> response = attractionController.create(req, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }

    @Test
    @DisplayName("Update deve retornar atração atualizada")
    void testUpdate() {
        AttractionRequestDTO req = new AttractionRequestDTO();
        when(attractionService.update(attrId, req, currentUser.getId())).thenReturn(responseDTO);

        ResponseEntity<AttractionResponseDTO> response = attractionController.update(attrId, req, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Delete deve retornar 204")
    void testDelete() {
        doNothing().when(attractionService).delete(attrId, currentUser.getId());

        ResponseEntity<Void> response = attractionController.delete(attrId, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Find All deve retornar pagina de atrações")
    void testFindAll() {
        Pageable pageable = Pageable.unpaged();
        Page<AttractionResponseDTO> page = new PageImpl<>(List.of(responseDTO));
        when(attractionService.findAll(pageable)).thenReturn(page);

        ResponseEntity<Page<AttractionResponseDTO>> response = attractionController.findAll(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Find By Id deve retornar atração")
    void testFindById() {
        when(attractionService.findById(attrId)).thenReturn(responseDTO);

        ResponseEntity<AttractionResponseDTO> response = attractionController.findById(attrId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Get Nearby deve retornar lista")
    void testGetNearby() {
        when(attractionService.getNearbyAttractions(10.0, 10.0, 50.0)).thenReturn(List.of(responseDTO));

        ResponseEntity<List<AttractionResponseDTO>> response = attractionController.getNearby(10.0, 10.0, 50.0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Search deve retornar lista")
    void testSearch() {
        when(attractionService.searchAttractions("kw", 10.0, 10.0, 50.0)).thenReturn(List.of(responseDTO));

        ResponseEntity<List<AttractionResponseDTO>> response = attractionController.search("kw", 10.0, 10.0, 50.0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
