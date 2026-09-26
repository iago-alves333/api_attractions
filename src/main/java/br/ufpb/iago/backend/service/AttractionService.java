package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.mapper.AttractionMapper;

import br.ufpb.iago.backend.dto.AttractionRequestDTO;
import br.ufpb.iago.backend.dto.AttractionResponseDTO;
import br.ufpb.iago.backend.dto.PageDTO;
import br.ufpb.iago.backend.exception.AttractionNotFoundException;
import br.ufpb.iago.backend.exception.GuideNotFoundException;
import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.AttractionRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class AttractionService {

    private static final int SRID = 4326;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);

    private final AttractionRepository attractionRepository;
    private final UserRepository userRepository;
    private final AttractionMapper attractionMapper;

    public AttractionService(AttractionRepository attractionRepository, UserRepository userRepository, AttractionMapper attractionMapper) {
        this.attractionRepository = attractionRepository;
        this.userRepository = userRepository;
        this.attractionMapper = attractionMapper;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @CacheEvict(value = "attractions", allEntries = true)
    @Transactional
    public AttractionResponseDTO create(AttractionRequestDTO dto, UUID guideId) {
        User guide = userRepository.findById(guideId)
                .orElseThrow(GuideNotFoundException::new);

        Attraction attraction = new Attraction();
        attraction.setGuide(guide);
        applyDto(attraction, dto);

        return attractionMapper.toResponseDTO(attractionRepository.save(attraction));
    }

    // ─── READ ─────────────────────────────────────────────────────────────────
    @Cacheable(value = "attractions", key = "'page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PageDTO<AttractionResponseDTO> findAll(Pageable pageable) {
        return new PageDTO<>(attractionRepository.findAll(pageable)
                .map(attractionMapper::toResponseDTO));
    }

    @Cacheable(value = "attractions", key = "#id")
    @Transactional(readOnly = true)
    public AttractionResponseDTO findById(UUID id) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(AttractionNotFoundException::new);
        return attractionMapper.toResponseDTO(attraction);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────
    @CacheEvict(value = "attractions", allEntries = true)
    @Transactional
    public AttractionResponseDTO update(UUID id, AttractionRequestDTO dto, UUID guideId) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(AttractionNotFoundException::new);

        if (!attraction.getGuide().getId().equals(guideId)) {
            throw new AccessDeniedException("Você não tem permissão para editar esta atração");
        }

        applyDto(attraction, dto);
        return attractionMapper.toResponseDTO(attractionRepository.save(attraction));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @CacheEvict(value = "attractions", allEntries = true)
    @Transactional
    public void delete(UUID id, UUID guideId) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(AttractionNotFoundException::new);

        if (!attraction.getGuide().getId().equals(guideId)) {
            throw new AccessDeniedException("Você não tem permissão para deletar esta atração");
        }

        attractionRepository.delete(attraction);
    }

    // ─── SEARCH ───────────────────────────────────────────────────────────────
    public PageDTO<AttractionResponseDTO> searchByTitle(String title, Pageable pageable) {
        return new PageDTO<>(attractionRepository.findByTitleContainingIgnoreCase(title, pageable)
                .map(attractionMapper::toResponseDTO));
    }
    public PageDTO<AttractionResponseDTO> getNearbyAttractions(double lat,double lon, double radiusKm, Pageable pageable){
        double radiusInMeters = (radiusKm > 0 ? radiusKm : 50.0) * 1000;
        Page<Attraction> attractions = attractionRepository.findNearby(lat, lon, radiusInMeters, pageable);
        return new PageDTO<>(attractions.map(attractionMapper::toResponseDTO));
    }
    public PageDTO<AttractionResponseDTO> searchAttractions(String keyword, double lat, double lon, double radiusKm, Pageable pageable) {
        double radiusInMeters = (radiusKm > 0 ? radiusKm : 50.0) * 1000;
        Page<Attraction> attractions = attractionRepository.searchByKeywordAndLocation(keyword, lat, lon, radiusInMeters, pageable);
        return new PageDTO<>(attractions.map(attractionMapper::toResponseDTO));
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private void applyDto(Attraction attraction, AttractionRequestDTO dto) {
        attractionMapper.updateEntityFromDTO(dto, attraction);

        Point point = geometryFactory.createPoint(
                new Coordinate(dto.getLongitude(), dto.getLatitude())
        );
        attraction.setLocation(point);
    }

}
