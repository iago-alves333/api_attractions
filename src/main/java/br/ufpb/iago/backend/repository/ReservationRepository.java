package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Reservation;
import br.ufpb.iago.backend.model.Status;
import br.ufpb.iago.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findAllByTouristId(UUID touristId);
    List<Reservation> findAllByAttractionGuideId(UUID guideId);
    List<Reservation> findAllByAttractionId(UUID attractionId);
    boolean existsByTouristAndAttractionAndStatus(User tourist, Attraction attraction, Status status);
}