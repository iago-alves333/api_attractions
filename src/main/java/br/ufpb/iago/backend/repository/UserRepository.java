package br.ufpb.iago.backend.repository;


import br.ufpb.iago.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("""
            SELECT CASE WHEN (
                EXISTS (SELECT 1 FROM Attraction a WHERE a.guide.id = :userId) OR
                EXISTS (SELECT 1 FROM Reservation r WHERE r.tourist.id = :userId) OR
                EXISTS (SELECT 1 FROM Review rv WHERE rv.tourist.id = :userId)
            ) THEN true ELSE false END
            """)
    boolean hasLinkedRecords(@Param("userId") UUID userId);
}
