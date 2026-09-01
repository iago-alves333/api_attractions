package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AttractionRepository extends JpaRepository<Attraction, UUID> {

    @Query(value = """
            SELECT * FROM attractions a 
            WHERE ST_DWithin(
                CAST(a.location AS geography), 
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), 
                :radiusInMeters
            )
            """, nativeQuery = true)
    List<Attraction> findAttractionsWithinRadius(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusInMeters") double radiusInMeters
    );
}