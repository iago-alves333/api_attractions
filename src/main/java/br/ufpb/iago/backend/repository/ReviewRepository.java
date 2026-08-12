package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
}
