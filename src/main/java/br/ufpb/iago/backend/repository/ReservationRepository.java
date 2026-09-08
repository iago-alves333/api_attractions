package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Reservation;
import br.ufpb.iago.backend.model.Status;
import br.ufpb.iago.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Page<Reservation> findAllByTouristId(UUID touristId, Pageable pageable);
    Page<Reservation> findAllByAttractionGuideId(UUID guideId, Pageable pageable);
    Page<Reservation> findAllByAttractionId(UUID attractionId, Pageable pageable);
    boolean existsByTouristAndAttractionAndStatus(User tourist, Attraction attraction, Status status);
}