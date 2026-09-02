package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {
    boolean existsByToken(String token);
}
