package br.ufpb.iago.backend.repository;


import br.ufpb.iago.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {

}
