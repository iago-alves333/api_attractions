package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.ReviewRequestDTO;
import br.ufpb.iago.backend.dto.ReviewResponseDTO;
import br.ufpb.iago.backend.exception.ResourceNotFoundException;
import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Review;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.AttractionRepository;
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

    public ReviewService(ReviewRepository reviewRepository,
                         AttractionRepository attractionRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.attractionRepository = attractionRepository;
        this.userRepository = userRepository;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponseDTO create(ReviewRequestDTO dto, UUID touristId) {
        User tourist = userRepository.findById(touristId)
                .orElseThrow(() -> new ResourceNotFoundException("Turista não encontrado"));

        Attraction attraction = attractionRepository.findById(dto.getAttractionId())
                .orElseThrow(() -> new ResourceNotFoundException("Atração não encontrada"));

        Review review = new Review();
        review.setTourist(tourist);
        review.setAttraction(attraction);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        return convertToDTO(reviewRepository.save(review));
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
        return reviewRepository.findAll()
                .stream()
                .filter(r -> r.getAttraction().getId().equals(attractionId))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewResponseDTO findById(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review não encontrado"));
        return convertToDTO(review);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponseDTO update(UUID id, ReviewRequestDTO dto, UUID touristId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review não encontrado"));

        if (!review.getTourist().getId().equals(touristId)) {
            throw new AccessDeniedException("Você não tem permissão para editar este review");
        }

        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        return convertToDTO(reviewRepository.save(review));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id, UUID touristId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review não encontrado"));

        if (!review.getTourist().getId().equals(touristId)) {
            throw new AccessDeniedException("Você não tem permissão para deletar este review");
        }

        reviewRepository.delete(review);
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

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
