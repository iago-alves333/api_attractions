package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.ReservationRequestDTO;
import br.ufpb.iago.backend.dto.ReservationResponseDTO;
import br.ufpb.iago.backend.exception.BusinessException;
import br.ufpb.iago.backend.exception.ResourceNotFoundException;
import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Reservation;
import br.ufpb.iago.backend.model.Status;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.AttractionRepository;
import br.ufpb.iago.backend.repository.ReservationRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final AttractionRepository attractionRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              AttractionRepository attractionRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.attractionRepository = attractionRepository;
        this.userRepository = userRepository;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Transactional
    public ReservationResponseDTO create(ReservationRequestDTO dto, UUID touristId) {
        User tourist = userRepository.findById(touristId)
                .orElseThrow(() -> new ResourceNotFoundException("Turista não encontrado"));

        Attraction attraction = attractionRepository.findById(dto.getAttractionId())
                .orElseThrow(() -> new ResourceNotFoundException("Atração não encontrada"));

        if (dto.getReservedFor().isBefore(LocalDateTime.now())) {
            throw new BusinessException("A data da reserva deve ser no futuro");
        }

        if (attraction.getAvailableSpots() <= 0) {
            throw new BusinessException("Não há vagas disponíveis para esta atração");
        }

        if (attraction.getGuide() != null && attraction.getGuide().getId().equals(touristId)) {
            throw new BusinessException("O guia não pode reservar sua própria atração");
        }

        if (reservationRepository.existsByTouristAndAttractionAndStatus(tourist, attraction, Status.PENDING)) {
            throw new BusinessException("Você já possui uma reserva pendente para esta atração");
        }

        Reservation reservation = new Reservation();
        reservation.setTourist(tourist);
        reservation.setAttraction(attraction);
        reservation.setReservedFor(dto.getReservedFor());
        reservation.setStatus(Status.PENDING);

        // Decrementa vagas disponíveis
        attraction.setAvailableSpots(attraction.getAvailableSpots() - 1);
        attractionRepository.save(attraction);

        return convertToDTO(reservationRepository.save(reservation));
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> findAllByTourist(UUID touristId) {
        // Delega a filtragem diretamente para o banco de dados
        return reservationRepository.findAllByTouristId(touristId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationResponseDTO findById(UUID id, UUID touristId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

        if (!reservation.getTourist().getId().equals(touristId)) {
            throw new AccessDeniedException("Acesso negado a esta reserva");
        }

        return convertToDTO(reservation);
    }

    // ─── CANCEL ───────────────────────────────────────────────────────────────

    @Transactional
    public ReservationResponseDTO cancel(UUID id, UUID touristId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

        if (!reservation.getTourist().getId().equals(touristId)) {
            throw new AccessDeniedException("Você não tem permissão para cancelar esta reserva");
        }

        if (reservation.getStatus() != Status.PENDING && reservation.getStatus() != Status.CONFIRMED) {
            throw new BusinessException("Apenas reservas pendentes ou confirmadas podem ser canceladas");
        }

        reservation.setStatus(Status.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());

        // Devolve a vaga para a atração
        Attraction attraction = reservation.getAttraction();
        attraction.setAvailableSpots(attraction.getAvailableSpots() + 1);
        attractionRepository.save(attraction);

        return convertToDTO(reservationRepository.save(reservation));
    }

    // ─── CONFIRM ──────────────────────────────────────────────────────────────

    @Transactional
    public ReservationResponseDTO confirm(UUID id, UUID guideId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

        Attraction attraction = reservation.getAttraction();
        if (attraction.getGuide() == null || !attraction.getGuide().getId().equals(guideId)) {
            throw new AccessDeniedException("Apenas o guia da atração pode confirmar esta reserva");
        }

        if (reservation.getStatus() != Status.PENDING) {
            throw new BusinessException("Apenas reservas pendentes podem ser confirmadas");
        }

        reservation.setStatus(Status.CONFIRMED);
        return convertToDTO(reservationRepository.save(reservation));
    }

    // ─── COMPLETE ─────────────────────────────────────────────────────────────

    @Transactional
    public ReservationResponseDTO complete(UUID id, UUID guideId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

        Attraction attraction = reservation.getAttraction();
        if (attraction.getGuide() == null || !attraction.getGuide().getId().equals(guideId)) {
            throw new AccessDeniedException("Apenas o guia da atração pode completar esta reserva");
        }

        if (reservation.getStatus() != Status.CONFIRMED) {
            throw new BusinessException("Apenas reservas confirmadas podem ser completadas");
        }

        reservation.setStatus(Status.COMPLETED);
        return convertToDTO(reservationRepository.save(reservation));
    }

    // ─── GUIDE READ ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> findAllByGuide(UUID guideId) {
        return reservationRepository.findAllByAttractionGuideId(guideId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    public ReservationResponseDTO convertToDTO(Reservation reservation) {
        return new ReservationResponseDTO(
                reservation.getId(),
                reservation.getTourist().getId(),
                reservation.getAttraction().getId(),
                reservation.getStatus(),
                reservation.getReservedFor(),
                reservation.getCreatedAt()
        );
    }
}