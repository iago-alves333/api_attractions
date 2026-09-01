package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AttractionRepository extends JpaRepository<Attraction, UUID> {

    // 1. Busca por proximidade ordenada pela menor distância
    @Query(value = """
            SELECT * FROM attractions a 
            WHERE ST_DWithin(
                CAST(a.location AS geography), 
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), 
                :radiusInMeters
            )
            ORDER BY ST_Distance(
                CAST(a.location AS geography),
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
            ) ASC
            """, nativeQuery = true)
    List<Attraction> findNearby(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusInMeters") double radiusInMeters
    );

    // 2. Busca combinada (Texto ILIKE + Proximidade ordenada)
    @Query(value = """
            SELECT * FROM attractions a 
            WHERE (a.title ILIKE CONCAT('%', :keyword, '%') OR a.description ILIKE CONCAT('%', :keyword, '%'))
            AND ST_DWithin(
                CAST(a.location AS geography), 
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), 
                :radiusInMeters
            )
            ORDER BY ST_Distance(
                CAST(a.location AS geography),
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
            ) ASC
            """, nativeQuery = true)
    List<Attraction> searchByKeywordAndLocation(
            @Param("keyword") String keyword,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusInMeters") double radiusInMeters
    );
    List<Attraction> findByTitleContainingIgnoreCase(String title);
}