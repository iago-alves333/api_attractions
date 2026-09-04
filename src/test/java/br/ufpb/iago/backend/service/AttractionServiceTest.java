package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.AttractionRequestDTO;
import br.ufpb.iago.backend.dto.AttractionResponseDTO;
import br.ufpb.iago.backend.exception.AttractionNotFoundException;
import br.ufpb.iago.backend.exception.GuideNotFoundException;
import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.AttractionRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttractionServiceTest {

    @Mock
    private AttractionRepository attractionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AttractionService attractionService;

    private User guide;
    private Attraction attraction;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        guide = new User();
        guide.setId(UUID.randomUUID());
        guide.setName("Guia");
        guide.setRole(Role.GUIDE);

        attraction = new Attraction();
        attraction.setId(UUID.randomUUID());
        attraction.setTitle("Praia");
        attraction.setDescription("Praia bonita");
        attraction.setPrice(new BigDecimal("50.00"));
        attraction.setAvailableSpots(10);
        attraction.setGuide(guide);
        attraction.setLocation(geometryFactory.createPoint(new Coordinate(-34.8, -7.1)));
        attraction.setRatingAverage(4.5);
        attraction.setReviewCount(2);
    }

    private AttractionRequestDTO createRequestDTO() {
        AttractionRequestDTO dto = new AttractionRequestDTO();
        dto.setTitle("Nova Atração");
        dto.setDescription("Descrição");
        dto.setPrice(new BigDecimal("100.00"));
        dto.setAvailableSpots(20);
        dto.setLatitude(-7.1);
        dto.setLongitude(-34.8);
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Deve criar atração com sucesso")
        void create_sucesso() {
            AttractionRequestDTO dto = createRequestDTO();
            UUID guideId = guide.getId();

            when(userRepository.findById(guideId)).thenReturn(Optional.of(guide));
            when(attractionRepository.save(any(Attraction.class))).thenAnswer(i -> {
                Attraction saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            AttractionResponseDTO result = attractionService.create(dto, guideId);

            assertNotNull(result);
            assertEquals(dto.getTitle(), result.title());
            assertEquals(dto.getPrice(), result.price());
            verify(attractionRepository).save(any(Attraction.class));
        }

        @Test
        @DisplayName("Deve lançar GuideNotFoundException se guia não existir")
        void create_guiaNaoExiste_lancaExcecao() {
            AttractionRequestDTO dto = createRequestDTO();
            UUID guideId = UUID.randomUUID();

            when(userRepository.findById(guideId)).thenReturn(Optional.empty());

            assertThrows(GuideNotFoundException.class, () -> attractionService.create(dto, guideId));
            verify(attractionRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ (findAll, findById)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Read methods")
    class ReadTests {

        @Test
        @DisplayName("findAll() deve retornar Page de atrações")
        void findAll_sucesso() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Attraction> page = new PageImpl<>(List.of(attraction));

            when(attractionRepository.findAll(pageable)).thenReturn(page);

            Page<AttractionResponseDTO> result = attractionService.findAll(pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals(attraction.getId(), result.getContent().get(0).id());
        }

        @Test
        @DisplayName("findById() deve retornar atração quando existir")
        void findById_sucesso() {
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

            AttractionResponseDTO result = attractionService.findById(attraction.getId());

            assertNotNull(result);
            assertEquals(attraction.getId(), result.id());
        }

        @Test
        @DisplayName("findById() deve lançar AttractionNotFoundException quando não existir")
        void findById_naoEncontrada_lancaExcecao() {
            UUID id = UUID.randomUUID();
            when(attractionRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(AttractionNotFoundException.class, () -> attractionService.findById(id));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Deve atualizar atração com sucesso")
        void update_sucesso() {
            AttractionRequestDTO dto = createRequestDTO();
            UUID guideId = guide.getId();

            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
            when(attractionRepository.save(any(Attraction.class))).thenAnswer(i -> i.getArgument(0));

            AttractionResponseDTO result = attractionService.update(attraction.getId(), dto, guideId);

            assertNotNull(result);
            assertEquals(dto.getTitle(), result.title());
            assertEquals(dto.getPrice(), result.price());
        }

        @Test
        @DisplayName("Deve lançar AccessDeniedException se outro guia tentar editar")
        void update_outroGuia_lancaExcecao() {
            AttractionRequestDTO dto = createRequestDTO();
            UUID outroGuiaId = UUID.randomUUID();

            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

            assertThrows(AccessDeniedException.class, () -> attractionService.update(attraction.getId(), dto, outroGuiaId));
            verify(attractionRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar atração com sucesso")
        void delete_sucesso() {
            UUID guideId = guide.getId();

            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

            assertDoesNotThrow(() -> attractionService.delete(attraction.getId(), guideId));
            verify(attractionRepository).delete(attraction);
        }

        @Test
        @DisplayName("Deve lançar AccessDeniedException se outro guia tentar deletar")
        void delete_outroGuia_lancaExcecao() {
            UUID outroGuiaId = UUID.randomUUID();

            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

            assertThrows(AccessDeniedException.class, () -> attractionService.delete(attraction.getId(), outroGuiaId));
            verify(attractionRepository, never()).delete(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Search methods")
    class SearchTests {

        @Test
        @DisplayName("searchByTitle() deve retornar lista")
        void searchByTitle_sucesso() {
            when(attractionRepository.findByTitleContainingIgnoreCase("Praia")).thenReturn(List.of(attraction));

            List<AttractionResponseDTO> result = attractionService.searchByTitle("Praia");

            assertEquals(1, result.size());
            assertEquals("Praia", result.get(0).title());
        }

        @Test
        @DisplayName("getNearbyAttractions() deve usar o raio default se raio for negativo ou zero")
        void getNearbyAttractions_usaDefaultRadius() {
            double defaultRadiusMeters = 50.0 * 1000;
            when(attractionRepository.findNearby(-7.1, -34.8, defaultRadiusMeters)).thenReturn(List.of(attraction));

            List<AttractionResponseDTO> result = attractionService.getNearbyAttractions(-7.1, -34.8, 0);

            assertEquals(1, result.size());
            verify(attractionRepository).findNearby(-7.1, -34.8, defaultRadiusMeters);
        }

        @Test
        @DisplayName("getNearbyAttractions() deve usar o raio fornecido")
        void getNearbyAttractions_usaRaioFornecido() {
            double radiusKm = 10.0;
            double radiusMeters = 10.0 * 1000;
            when(attractionRepository.findNearby(-7.1, -34.8, radiusMeters)).thenReturn(List.of(attraction));

            List<AttractionResponseDTO> result = attractionService.getNearbyAttractions(-7.1, -34.8, radiusKm);

            assertEquals(1, result.size());
            verify(attractionRepository).findNearby(-7.1, -34.8, radiusMeters);
        }

        @Test
        @DisplayName("searchAttractions() deve buscar por termo e raio")
        void searchAttractions_sucesso() {
            double radiusMeters = 20.0 * 1000;
            when(attractionRepository.searchByKeywordAndLocation("Praia", -7.1, -34.8, radiusMeters)).thenReturn(List.of(attraction));

            List<AttractionResponseDTO> result = attractionService.searchAttractions("Praia", -7.1, -34.8, 20.0);

            assertEquals(1, result.size());
            verify(attractionRepository).searchByKeywordAndLocation("Praia", -7.1, -34.8, radiusMeters);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONVERT TO DTO
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("convertToDTO()")
    class ConvertToDTOTests {

        @Test
        @DisplayName("Deve converter atração com location e ratings corretamente")
        void convertToDTO_sucesso() {
            AttractionResponseDTO dto = attractionService.convertToDTO(attraction);

            assertEquals(attraction.getId(), dto.id());
            assertEquals(guide.getId(), dto.guideId());
            assertEquals(-34.8, dto.longitude());
            assertEquals(-7.1, dto.latitude());
            assertEquals(4.5, dto.ratingAverage());
            assertEquals(2, dto.reviewCount());
        }

        @Test
        @DisplayName("Deve lidar com valores nulos suavemente")
        void convertToDTO_comValoresNulos_lidarBem() {
            Attraction attNull = new Attraction();
            attNull.setId(UUID.randomUUID());
            attNull.setTitle("Sem Local");
            attNull.setPrice(BigDecimal.TEN);
            attNull.setAvailableSpots(5);
            attNull.setGuide(guide);
            // location = null, ratingAverage = null, reviewCount = null

            AttractionResponseDTO dto = attractionService.convertToDTO(attNull);

            assertEquals(0.0, dto.longitude());
            assertEquals(0.0, dto.latitude());
            assertEquals(0.0, dto.ratingAverage());
            assertEquals(0, dto.reviewCount());
        }
    }
}
