package br.ufpb.iago.backend.mapper;

import br.ufpb.iago.backend.dto.ReviewResponseDTO;
import br.ufpb.iago.backend.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(source = "attraction.id", target = "attractionId")
    @Mapping(source = "tourist.id", target = "touristId")
    @Mapping(source = "tourist.name", target = "touristName")
    ReviewResponseDTO toResponseDTO(Review review);
}
