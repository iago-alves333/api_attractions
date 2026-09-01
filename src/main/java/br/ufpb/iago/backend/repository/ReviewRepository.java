package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Review;
import br.ufpb.iago.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByTouristAndAttraction(User tourist, Attraction attraction);
    List<Review> findAllByAttractionId(UUID attractionId);
}
