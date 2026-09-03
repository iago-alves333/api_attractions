package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.ReviewRequestDTO;
import br.ufpb.iago.backend.dto.ReviewResponseDTO;
import br.ufpb.iago.backend.exception.AttractionNotFoundException;
import br.ufpb.iago.backend.exception.DuplicateReviewException;
import br.ufpb.iago.backend.exception.ReviewNotFoundException;
import br.ufpb.iago.backend.exception.UserNotFoundException;
import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Review;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.AttractionRepository;
import br.ufpb.iago.backend.repository.ReservationRepository;
import br.ufpb.iago.backend.repository.ReviewRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AttractionRepository attractionRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         AttractionRepository attractionRepository,
                         UserRepository userRepository,
                         ReservationRepository reservationRepository) {
        this.reviewRepository = reviewRepository;
        this.attractionRepository = attractionRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponseDTO create(ReviewRequestDTO dto, UUID touristId) {
        User tourist = userRepository.findById(touristId)
                .orElseThrow(UserNotFoundException::new);

        Attraction attraction = attractionRepository.findById(dto.getAttractionId())
                .orElseThrow(AttractionNotFoundException::new);

        if (reviewRepository.existsByTouristAndAttraction(tourist, attraction)) {
            throw new DuplicateReviewException();
        }
        if (!reservationRepository.existsByTouristAndAttractionAndStatus(tourist, attraction, br.ufpb.iago.backend.model.Status.COMPLETED)) {
            throw new AccessDeniedException("Você só pode avaliar atrações que você reservou e completou");
        }
        if (attraction.getGuide() != null && attraction.getGuide().getId().equals(touristId)) {
            throw new AccessDeniedException("O guia não pode avaliar sua própria atração");
        }

        Review review = new Review();
        review.setTourist(tourist);
        review.setAttraction(attraction);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        review = reviewRepository.save(review);

        // Atualiza a média e a contagem na atração
        updateAttractionRating(attraction);

        return convertToDTO(review);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> findAll() {
        return reviewRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> findByAttraction(UUID attractionId) {
        return reviewRepository.findAllByAttractionId(attractionId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewResponseDTO findById(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(ReviewNotFoundException::new);
        return convertToDTO(review);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponseDTO update(UUID id, ReviewRequestDTO dto, UUID touristId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(ReviewNotFoundException::new);

        if (!review.getTourist().getId().equals(touristId)) {
            throw new AccessDeniedException("Você não tem permissão para editar este review");
        }

        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        review = reviewRepository.save(review);

        // Atualiza a média na atração após a edição
        updateAttractionRating(review.getAttraction());

        return convertToDTO(review);
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id, UUID touristId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(ReviewNotFoundException::new);

        if (!review.getTourist().getId().equals(touristId)) {
            throw new AccessDeniedException("Você não tem permissão para deletar este review");
        }

        Attraction attraction = review.getAttraction();

        reviewRepository.delete(review);
        // Força a exclusão no banco antes de recalcular a média
        reviewRepository.flush();

        // Atualiza a média na atração após a exclusão
        updateAttractionRating(attraction);
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private void updateAttractionRating(Attraction attraction) {
        Double avg = reviewRepository.getAverageRatingByAttractionId(attraction.getId());
        Integer count = reviewRepository.countByAttractionId(attraction.getId());

        // Se avg for null (ex: quando o último review for deletado), define como 0.0
        if (avg == null) {
            avg = 0.0;
        }

        // Arredonda para 1 casa decimal (ex: 4.33333 -> 4.3)
        double roundedAvg = Math.round(avg * 10.0) / 10.0;

        attraction.setRatingAverage(roundedAvg);
        // Assumindo que você criou o campo reviewCount na entidade Attraction
        attraction.setReviewCount(count != null ? count : 0);

        attractionRepository.save(attraction);
    }

    public ReviewResponseDTO convertToDTO(Review review) {
        return new ReviewResponseDTO(
                review.getId(),
                review.getAttraction().getId(),
                review.getTourist().getId(),
                review.getTourist().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}