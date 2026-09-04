package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.ReviewRequestDTO;
import br.ufpb.iago.backend.dto.ReviewResponseDTO;
import br.ufpb.iago.backend.exception.AttractionNotFoundException;
import br.ufpb.iago.backend.exception.DuplicateReviewException;
import br.ufpb.iago.backend.exception.ReviewNotFoundException;
import br.ufpb.iago.backend.exception.UserNotFoundException;
import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Review;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.Status;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.AttractionRepository;
import br.ufpb.iago.backend.repository.ReservationRepository;
import br.ufpb.iago.backend.repository.ReviewRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AttractionRepository attractionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User tourist;
    private User guide;
    private Attraction attraction;
    private Review review;

    @BeforeEach
    void setUp() {
        tourist = new User();
        tourist.setId(UUID.randomUUID());
        tourist.setName("Turista");
        tourist.setRole(Role.TOURIST);

        guide = new User();
        guide.setId(UUID.randomUUID());
        guide.setName("Guia");
        guide.setRole(Role.GUIDE);

        attraction = new Attraction();
        attraction.setId(UUID.randomUUID());
        attraction.setTitle("Praia");
        attraction.setGuide(guide);
        attraction.setRatingAverage(0.0);
        attraction.setReviewCount(0);

        review = new Review();
        review.setId(UUID.randomUUID());
        review.setTourist(tourist);
        review.setAttraction(attraction);
        review.setRating(5);
        review.setComment("Ótimo lugar");
    }

    private ReviewRequestDTO createRequestDTO(UUID attractionId, int rating, String comment) {
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setAttractionId(attractionId);
        dto.setRating(rating);
        dto.setComment(comment);
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Deve criar review com sucesso")
        void create_sucesso() {
            ReviewRequestDTO dto = createRequestDTO(attraction.getId(), 5, "Muito bom");
            UUID touristId = tourist.getId();

            when(userRepository.findById(touristId)).thenReturn(Optional.of(tourist));
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
            when(reviewRepository.existsByTouristAndAttraction(tourist, attraction)).thenReturn(false);
            when(reservationRepository.existsByTouristAndAttractionAndStatus(tourist, attraction, Status.COMPLETED)).thenReturn(true);
            when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
                Review r = i.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(reviewRepository.getAverageRatingByAttractionId(attraction.getId())).thenReturn(5.0);
            when(reviewRepository.countByAttractionId(attraction.getId())).thenReturn(1);

            ReviewResponseDTO result = reviewService.create(dto, touristId);

            assertNotNull(result);
            assertEquals(5, result.rating());
            assertEquals("Muito bom", result.comment());
            verify(attractionRepository).save(attraction);
            assertEquals(5.0, attraction.getRatingAverage());
            assertEquals(1, attraction.getReviewCount());
        }

        @Test
        @DisplayName("Deve lançar UserNotFoundException quando turista não for encontrado")
        void create_touristNaoEncontrado_lancaExcecao() {
            ReviewRequestDTO dto = createRequestDTO(attraction.getId(), 5, "Muito bom");
            UUID touristId = UUID.randomUUID();

            when(userRepository.findById(touristId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> reviewService.create(dto, touristId));
        }

        @Test
        @DisplayName("Deve lançar AttractionNotFoundException quando atração não for encontrada")
        void create_attractionNaoEncontrada_lancaExcecao() {
            ReviewRequestDTO dto = createRequestDTO(UUID.randomUUID(), 5, "Muito bom");
            UUID touristId = tourist.getId();

            when(userRepository.findById(touristId)).thenReturn(Optional.of(tourist));
            when(attractionRepository.findById(dto.getAttractionId())).thenReturn(Optional.empty());

            assertThrows(AttractionNotFoundException.class, () -> reviewService.create(dto, touristId));
        }

        @Test
        @DisplayName("Deve lançar DuplicateReviewException quando já houver review do usuário")
        void create_reviewDuplicado_lancaExcecao() {
            ReviewRequestDTO dto = createRequestDTO(attraction.getId(), 5, "Muito bom");
            UUID touristId = tourist.getId();

            when(userRepository.findById(touristId)).thenReturn(Optional.of(tourist));
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
            when(reviewRepository.existsByTouristAndAttraction(tourist, attraction)).thenReturn(true);

            assertThrows(DuplicateReviewException.class, () -> reviewService.create(dto, touristId));
        }

        @Test
        @DisplayName("Deve lançar AccessDeniedException quando reserva não estiver completa")
        void create_reservaNaoCompleta_lancaExcecao() {
            ReviewRequestDTO dto = createRequestDTO(attraction.getId(), 5, "Muito bom");
            UUID touristId = tourist.getId();

            when(userRepository.findById(touristId)).thenReturn(Optional.of(tourist));
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
            when(reviewRepository.existsByTouristAndAttraction(tourist, attraction)).thenReturn(false);
            when(reservationRepository.existsByTouristAndAttractionAndStatus(tourist, attraction, Status.COMPLETED)).thenReturn(false);

            assertThrows(AccessDeniedException.class, () -> reviewService.create(dto, touristId));
        }

        @Test
        @DisplayName("Deve lançar AccessDeniedException quando guia tenta avaliar a própria atração")
        void create_guiaAvaliaPropriaAtracao_lancaExcecao() {
            ReviewRequestDTO dto = createRequestDTO(attraction.getId(), 5, "Muito bom");
            UUID guideId = guide.getId(); // O guia tentará avaliar

            when(userRepository.findById(guideId)).thenReturn(Optional.of(guide));
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
            when(reviewRepository.existsByTouristAndAttraction(guide, attraction)).thenReturn(false);
            when(reservationRepository.existsByTouristAndAttractionAndStatus(guide, attraction, Status.COMPLETED)).thenReturn(true);

            assertThrows(AccessDeniedException.class, () -> reviewService.create(dto, guideId));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Read methods")
    class ReadTests {

        @Test
        @DisplayName("findAll() deve retornar lista de reviews")
        void findAll_sucesso() {
            when(reviewRepository.findAll()).thenReturn(List.of(review));

            List<ReviewResponseDTO> result = reviewService.findAll();

            assertEquals(1, result.size());
            assertEquals(review.getId(), result.get(0).id());
        }

        @Test
        @DisplayName("findByAttraction() deve retornar lista de reviews da atração")
        void findByAttraction_sucesso() {
            when(reviewRepository.findAllByAttractionId(attraction.getId())).thenReturn(List.of(review));

            List<ReviewResponseDTO> result = reviewService.findByAttraction(attraction.getId());

            assertEquals(1, result.size());
            assertEquals(review.getId(), result.get(0).id());
        }

        @Test
        @DisplayName("findById() deve retornar o review quando existe")
        void findById_sucesso() {
            when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

            ReviewResponseDTO result = reviewService.findById(review.getId());

            assertEquals(review.getId(), result.id());
        }

        @Test
        @DisplayName("findById() deve lançar exceção quando não existe")
        void findById_naoEncontrado_lancaExcecao() {
            UUID id = UUID.randomUUID();
            when(reviewRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ReviewNotFoundException.class, () -> reviewService.findById(id));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Deve atualizar review com sucesso")
        void update_sucesso() {
            ReviewRequestDTO dto = createRequestDTO(attraction.getId(), 4, "Atualizado");
            UUID touristId = tourist.getId();

            when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
            when(reviewRepository.getAverageRatingByAttractionId(attraction.getId())).thenReturn(4.0);
            when(reviewRepository.countByAttractionId(attraction.getId())).thenReturn(1);

            ReviewResponseDTO result = reviewService.update(review.getId(), dto, touristId);

            assertNotNull(result);
            assertEquals(4, result.rating());
            assertEquals("Atualizado", result.comment());
            assertEquals(4.0, attraction.getRatingAverage());
        }

        @Test
        @DisplayName("Deve lançar AccessDeniedException se tentar atualizar review de outro usuário")
        void update_outroUsuario_lancaExcecao() {
            ReviewRequestDTO dto = createRequestDTO(attraction.getId(), 4, "Atualizado");
            UUID outroUsuarioId = UUID.randomUUID();

            when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

            assertThrows(AccessDeniedException.class, () -> reviewService.update(review.getId(), dto, outroUsuarioId));
        }

        @Test
        @DisplayName("Deve lançar ReviewNotFoundException se o review não existir")
        void update_naoEncontrado_lancaExcecao() {
            ReviewRequestDTO dto = createRequestDTO(attraction.getId(), 4, "Atualizado");
            UUID id = UUID.randomUUID();
            UUID touristId = tourist.getId();

            when(reviewRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ReviewNotFoundException.class, () -> reviewService.update(id, dto, touristId));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar review com sucesso")
        void delete_sucesso() {
            UUID touristId = tourist.getId();

            when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
            when(reviewRepository.getAverageRatingByAttractionId(attraction.getId())).thenReturn(null);
            when(reviewRepository.countByAttractionId(attraction.getId())).thenReturn(0);

            assertDoesNotThrow(() -> reviewService.delete(review.getId(), touristId));
            
            verify(reviewRepository).delete(review);
            verify(reviewRepository).flush();
            verify(attractionRepository).save(attraction);
            
            assertEquals(0.0, attraction.getRatingAverage());
            assertEquals(0, attraction.getReviewCount());
        }

        @Test
        @DisplayName("Deve lançar AccessDeniedException se tentar deletar review de outro usuário")
        void delete_outroUsuario_lancaExcecao() {
            UUID outroUsuarioId = UUID.randomUUID();

            when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

            assertThrows(AccessDeniedException.class, () -> reviewService.delete(review.getId(), outroUsuarioId));
        }

        @Test
        @DisplayName("Deve lançar ReviewNotFoundException se o review não existir")
        void delete_naoEncontrado_lancaExcecao() {
            UUID id = UUID.randomUUID();
            UUID touristId = tourist.getId();

            when(reviewRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ReviewNotFoundException.class, () -> reviewService.delete(id, touristId));
        }
    }
}
