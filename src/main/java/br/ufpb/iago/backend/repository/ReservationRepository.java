package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Reservation;
import br.ufpb.iago.backend.model.Status;
import br.ufpb.iago.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    @Override
    @EntityGraph(attributePaths = {"tourist", "attraction"})
    Optional<Reservation> findById(UUID id);

    @EntityGraph(attributePaths = {"tourist", "attraction"})
    Page<Reservation> findAllByTouristId(UUID touristId, Pageable pageable);

    @EntityGraph(attributePaths = {"tourist", "attraction"})
    Page<Reservation> findAllByAttractionGuideId(UUID guideId, Pageable pageable);

    @EntityGraph(attributePaths = {"tourist", "attraction"})
    Page<Reservation> findAllByAttractionId(UUID attractionId, Pageable pageable);

    boolean existsByTouristAndAttractionAndStatus(User tourist, Attraction attraction, Status status);
    long countByAttractionIdAndStatusInAndReservedForBetween(UUID attractionId, Collection<Status> statuses, LocalDateTime start, LocalDateTime end);
}