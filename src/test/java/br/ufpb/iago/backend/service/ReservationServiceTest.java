package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.ReservationRequestDTO;
import br.ufpb.iago.backend.model.*;
import br.ufpb.iago.backend.repository.AttractionRepository;
import br.ufpb.iago.backend.repository.ReservationRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
public class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AttractionRepository attractionRepository;

    @Mock

    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User tourist;
    private User guide;
    private Attraction attraction;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        tourist = new User();
        tourist.setId(java.util.UUID.randomUUID());
        tourist.setEmail("tourist@example.com");
        tourist.setRole(Role.TOURIST);

        guide = new User();
        guide.setId(java.util.UUID.randomUUID());
        guide.setEmail("guide@test.com");
        guide.setRole(Role.GUIDE);

        attraction = new Attraction();
        attraction.setId(java.util.UUID.randomUUID());
        attraction.setTitle("Praia");
        attraction.setGuide(guide);

        reservation = new Reservation();
        reservation.setId(java.util.UUID.randomUUID());
        reservation.setAttraction(attraction);
        reservation.setTourist(tourist);
        reservation.setReservedFor(LocalDateTime.now());

    }

    private ReservationRequestDTO createReservationRequestDTO(UUID attractionId, LocalDateTime reservedFor) {
        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setAttractionId(attraction.getId());
        requestDTO.setReservedFor(reservedFor);
        return requestDTO;
    }

    //────Create─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("create()")
    class CreateTests {
        @Test
        @DisplayName("should create a reservation successfully")
        void testCreateReservationSuccess() {
            ReservationRequestDTO requestDTO = createReservationRequestDTO(UUID.randomUUID(), LocalDateTime.now());
            UUID attractionId = UUID.randomUUID();
            
        }
    }
}