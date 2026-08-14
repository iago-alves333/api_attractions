package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.AttractionRequestDTO;
import br.ufpb.iago.backend.dto.AttractionResponseDTO;
import br.ufpb.iago.backend.exception.ResourceNotFoundException;
import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.AttractionRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttractionService {

    private static final int SRID = 4326;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);

    private final AttractionRepository attractionRepository;
    private final UserRepository userRepository;

    public AttractionService(AttractionRepository attractionRepository, UserRepository userRepository) {
        this.attractionRepository = attractionRepository;
        this.userRepository = userRepository;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Transactional
    public AttractionResponseDTO create(AttractionRequestDTO dto, UUID guideId) {
        User guide = userRepository.findById(guideId)
                .orElseThrow(() -> new ResourceNotFoundException("Guia não encontrado"));

        Attraction attraction = new Attraction();
        attraction.setGuide(guide);
        applyDto(attraction, dto);

        return convertToDTO(attractionRepository.save(attraction));
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AttractionResponseDTO> findAll() {
        return attractionRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttractionResponseDTO findById(UUID id) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atração não encontrada"));
        return convertToDTO(attraction);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Transactional
    public AttractionResponseDTO update(UUID id, AttractionRequestDTO dto, UUID guideId) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atração não encontrada"));

        if (!attraction.getGuide().getId().equals(guideId)) {
            throw new AccessDeniedException("Você não tem permissão para editar esta atração");
        }

        applyDto(attraction, dto);
        return convertToDTO(attractionRepository.save(attraction));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id, UUID guideId) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atração não encontrada"));

        if (!attraction.getGuide().getId().equals(guideId)) {
            throw new AccessDeniedException("Você não tem permissão para deletar esta atração");
        }

        attractionRepository.delete(attraction);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private void applyDto(Attraction attraction, AttractionRequestDTO dto) {
        attraction.setTitle(dto.getTitle());
        attraction.setDescription(dto.getDescription());
        attraction.setPrice(dto.getPrice());
        attraction.setAvailableSpots(dto.getAvailableSpots());

        Point point = geometryFactory.createPoint(
                new Coordinate(dto.getLongitude(), dto.getLatitude())
        );
        attraction.setLocation(point);
    }

    public AttractionResponseDTO convertToDTO(Attraction attraction) {
        double lat = attraction.getLocation() != null ? attraction.getLocation().getY() : 0.0;
        double lon = attraction.getLocation() != null ? attraction.getLocation().getX() : 0.0;

        return new AttractionResponseDTO(
                attraction.getId(),
                attraction.getGuide().getId(),
                attraction.getGuide().getName(),
                attraction.getTitle(),
                attraction.getDescription(),
                attraction.getPrice(),
                attraction.getAvailableSpots(),
                lat,
                lon
        );
    }
}
