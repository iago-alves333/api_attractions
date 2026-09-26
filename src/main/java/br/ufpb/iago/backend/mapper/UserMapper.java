package br.ufpb.iago.backend.mapper;

import br.ufpb.iago.backend.dto.UserResponseDTO;
import br.ufpb.iago.backend.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDTO toResponseDTO(User user);
}
