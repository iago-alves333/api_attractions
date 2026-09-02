package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Review;
import br.ufpb.iago.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByTouristAndAttraction(User tourist, Attraction attraction);
    List<Review> findAllByAttractionId(UUID attractionId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.attraction.getId() = :attractionId")
    Double getAverageRatingByAttractionId(@Param("attractionId") UUID attractionId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.attraction.getId() = :attractionId")
    Integer countByAttractionId(@Param("attractionId") UUID attractionId);
}
