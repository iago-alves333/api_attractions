package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.RefreshToken;
import br.ufpb.iago.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    void deleteAllByUser(User user);
    void deleteByExpiresAtBefore(LocalDateTime now);
}
