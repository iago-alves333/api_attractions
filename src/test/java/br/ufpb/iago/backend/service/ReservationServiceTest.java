package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.ReservationRequestDTO;
import br.ufpb.iago.backend.dto.ReservationResponseDTO;
import br.ufpb.iago.backend.exception.*;
import br.ufpb.iago.backend.model.*;
import br.ufpb.iago.backend.repository.AttractionRepository;
import br.ufpb.iago.backend.repository.ReservationRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

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
    private User anotherUser;
    private Attraction attraction;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        tourist = new User();
        tourist.setId(UUID.randomUUID());
        tourist.setEmail("tourist@example.com");
        tourist.setRole(Role.TOURIST);

        guide = new User();
        guide.setId(UUID.randomUUID());
        guide.setEmail("guide@example.com");
        guide.setRole(Role.GUIDE);

        anotherUser = new User();
        anotherUser.setId(UUID.randomUUID());
        anotherUser.setEmail("another@example.com");
        anotherUser.setRole(Role.TOURIST);

        attraction = new Attraction();
        attraction.setId(UUID.randomUUID());
        attraction.setTitle("Praia");
        attraction.setGuide(guide);
        attraction.setAvailableSpots(3);

        reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setAttraction(attraction);
        reservation.setTourist(tourist);
        reservation.setReservedFor(LocalDateTime.now().plusDays(1));
        reservation.setStatus(Status.PENDING);
    }

    private ReservationRequestDTO createReservationRequestDTO(UUID attractionId, LocalDateTime reservedFor) {
        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setAttractionId(attractionId);
        requestDTO.setReservedFor(reservedFor);
        return requestDTO;
    }

    private Reservation createSavedReservation() {
        Reservation saved = new Reservation();
        saved.setId(UUID.randomUUID());
        saved.setTourist(tourist);
        saved.setAttraction(attraction);
        saved.setReservedFor(LocalDateTime.now().plusDays(2));
        saved.setStatus(Status.PENDING);
        return saved;
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("deve criar reserva com sucesso")
        void create_comDadosValidos_criaReserva() {
            UUID touristId = tourist.getId();
            LocalDateTime reservedFor = LocalDateTime.now().plusDays(2);
            ReservationRequestDTO dto = createReservationRequestDTO(attraction.getId(), reservedFor);

            when(userRepository.findById(touristId)).thenReturn(Optional.of(tourist));
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
            when(reservationRepository.existsByTouristAndAttractionAndStatus(tourist, attraction, Status.PENDING)).thenReturn(false);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ReservationResponseDTO result = reservationService.create(dto, touristId);

            assertNotNull(result);
            assertEquals(touristId, result.touristId());
            assertEquals(attraction.getId(), result.attractionID());
            assertEquals(Status.PENDING, result.status());
            assertEquals(reservedFor, result.reservedFor());
            assertEquals(2, attraction.getAvailableSpots());
            verify(attractionRepository).save(attraction);
            verify(reservationRepository).save(any(Reservation.class));
        }

        @Test
        @DisplayName("deve lançar ReservationDateInPastException quando a data já passou")
        void create_comDataNoPassado_lancaException() {
            ReservationRequestDTO dto = createReservationRequestDTO(attraction.getId(), LocalDateTime.now().minusDays(1));

            when(userRepository.findById(tourist.getId())).thenReturn(Optional.of(tourist));
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

            assertThrows(ReservationDateInPastException.class, () -> reservationService.create(dto, tourist.getId()));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar NoAvailableSpotsException quando não há vagas")
        void create_semVagas_lancaException() {
            ReservationRequestDTO dto = createReservationRequestDTO(attraction.getId(), LocalDateTime.now().plusDays(1));
            attraction.setAvailableSpots(0);

            when(userRepository.findById(tourist.getId())).thenReturn(Optional.of(tourist));
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

            assertThrows(NoAvailableSpotsException.class, () -> reservationService.create(dto, tourist.getId()));
            verify(attractionRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar SelfReservationException quando turista é o guia")
        void create_turistaEGuiaDaAtracao_lancaException() {
            ReservationRequestDTO dto = createReservationRequestDTO(attraction.getId(), LocalDateTime.now().plusDays(1));

            when(userRepository.findById(guide.getId())).thenReturn(Optional.of(guide));
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

            assertThrows(SelfReservationException.class, () -> reservationService.create(dto, guide.getId()));
        }

        @Test
        @DisplayName("deve lançar DuplicateReservationException quando já existe reserva pendente")
        void create_reservaDuplicada_lancaException() {
            ReservationRequestDTO dto = createReservationRequestDTO(attraction.getId(), LocalDateTime.now().plusDays(1));

            when(userRepository.findById(tourist.getId())).thenReturn(Optional.of(tourist));
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
            when(reservationRepository.existsByTouristAndAttractionAndStatus(tourist, attraction, Status.PENDING)).thenReturn(true);

            assertThrows(DuplicateReservationException.class, () -> reservationService.create(dto, tourist.getId()));
            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("deve retornar reserva quando o turista é o dono")
        void findById_reservaDoTurista_retornaReserva() {
            reservation.setId(UUID.randomUUID());

            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

            ReservationResponseDTO result = reservationService.findById(reservation.getId(), tourist.getId());

            assertNotNull(result);
            assertEquals(reservation.getId(), result.id());
            assertEquals(tourist.getId(), result.touristId());
        }

        @Test
        @DisplayName("deve lançar AccessDeniedException quando turista não é dono da reserva")
        void findById_turistaDiferente_lancaAccessDenied() {
            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

            assertThrows(AccessDeniedException.class, () -> reservationService.findById(reservation.getId(), anotherUser.getId()));
        }
    }

    @Nested
    @DisplayName("cancel()")
    class CancelTests {

        @Test
        @DisplayName("deve cancelar reserva pendente e devolver a vaga")
        void cancel_reservaPendende_retornaReservaCancelada() {
            reservation.setStatus(Status.PENDING);
            attraction.setAvailableSpots(1);

            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(attractionRepository.save(any(Attraction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReservationResponseDTO result = reservationService.cancel(reservation.getId(), tourist.getId());

            assertEquals(Status.CANCELLED, result.status());
            assertEquals(2, attraction.getAvailableSpots());
            assertNotNull(reservation.getCancelledAt());
            verify(attractionRepository).save(attraction);
        }

        @Test
        @DisplayName("deve lançar AccessDeniedException quando usuário não é dono da reserva")
        void cancel_turistaDiferente_lancaAccessDenied() {
            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

            assertThrows(AccessDeniedException.class, () -> reservationService.cancel(reservation.getId(), anotherUser.getId()));
        }

        @Test
        @DisplayName("deve lançar InvalidReservationStateException para status inválido")
        void cancel_statusInvalido_lancaException() {
            reservation.setStatus(Status.COMPLETED);

            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

            assertThrows(InvalidReservationStateException.class, () -> reservationService.cancel(reservation.getId(), tourist.getId()));
        }
    }

    @Nested
    @DisplayName("confirm()")
    class ConfirmTests {

        @Test
        @DisplayName("deve confirmar reserva pendente pelo guia da atração")
        void confirm_reservaPendenteDoGuia_confirmaReserva() {
            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReservationResponseDTO result = reservationService.confirm(reservation.getId(), guide.getId());

            assertEquals(Status.CONFIRMED, result.status());
            assertEquals(Status.CONFIRMED, reservation.getStatus());
        }

        @Test
        @DisplayName("deve lançar AccessDeniedException quando guia não é dono da atração")
        void confirm_guiaSemPermissao_lancaAccessDenied() {
            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

            assertThrows(AccessDeniedException.class, () -> reservationService.confirm(reservation.getId(), anotherUser.getId()));
        }

        @Test
        @DisplayName("deve lançar InvalidReservationStateException quando status não é pendente")
        void confirm_statusInvalido_lancaException() {
            reservation.setStatus(Status.CONFIRMED);
            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

            assertThrows(InvalidReservationStateException.class, () -> reservationService.confirm(reservation.getId(), guide.getId()));
        }
    }

    @Nested
    @DisplayName("complete()")
    class CompleteTests {

        @Test
        @DisplayName("deve completar reserva confirmada pelo guia")
        void complete_reservaConfirmada_completaReserva() {
            reservation.setStatus(Status.CONFIRMED);
            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReservationResponseDTO result = reservationService.complete(reservation.getId(), guide.getId());

            assertEquals(Status.COMPLETED, result.status());
            assertEquals(Status.COMPLETED, reservation.getStatus());
        }

        @Test
        @DisplayName("deve lançar InvalidReservationStateException quando status não é confirmado")
        void complete_statusInvalido_lancaException() {
            reservation.setStatus(Status.PENDING);
            when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

            assertThrows(InvalidReservationStateException.class, () -> reservationService.complete(reservation.getId(), guide.getId()));
        }
    }

    @Nested
    @DisplayName("findAllByGuide()")
    class FindAllByGuideTests {

        @Test
        @DisplayName("deve retornar todas as reservas do guia")
        void findAllByGuide_retornaLista() {
            Reservation other = createSavedReservation();
            other.setTourist(anotherUser);
            other.setStatus(Status.CONFIRMED);

            when(reservationRepository.findAllByAttractionGuideId(guide.getId())).thenReturn(List.of(reservation, other));

            List<ReservationResponseDTO> result = reservationService.findAllByGuide(guide.getId());

            assertEquals(2, result.size());
            assertEquals(Status.PENDING, result.get(0).status());
            assertEquals(Status.CONFIRMED, result.get(1).status());
        }
    }

    @Nested
    @DisplayName("findAllByAttraction()")
    class FindAllByAttractionTests {

        @Test
        @DisplayName("deve retornar reservas da atração quando o guia é o dono")
        void findAllByAttraction_guiaDono_retornaReservas() {
            Reservation other = createSavedReservation();
            other.setTourist(anotherUser);
            other.setStatus(Status.CONFIRMED);

            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
            when(reservationRepository.findAllByAttractionId(attraction.getId())).thenReturn(List.of(reservation, other));

            List<ReservationResponseDTO> result = reservationService.findAllByAttraction(attraction.getId(), guide.getId());

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("deve lançar AccessDeniedException quando guia não é dono da atração")
        void findAllByAttraction_guiaNaoDono_lancaAccessDenied() {
            when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));

            assertThrows(AccessDeniedException.class,
                    () -> reservationService.findAllByAttraction(attraction.getId(), anotherUser.getId()));
        }
    }
}