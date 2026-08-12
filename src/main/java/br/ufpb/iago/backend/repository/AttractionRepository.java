package br.ufpb.iago.backend.repository;

import br.ufpb.iago.backend.model.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttractionRepository extends JpaRepository<Attraction, UUID> {
}
