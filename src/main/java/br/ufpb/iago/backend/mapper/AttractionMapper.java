package br.ufpb.iago.backend.mapper;

import br.ufpb.iago.backend.dto.AttractionRequestDTO;
import br.ufpb.iago.backend.dto.AttractionResponseDTO;
import br.ufpb.iago.backend.model.Attraction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AttractionMapper {

    @Mapping(source = "guide.id", target = "guideId")
    @Mapping(source = "guide.name", target = "guideName")
    @Mapping(target = "latitude", expression = "java(attraction.getLocation() != null ? attraction.getLocation().getY() : 0.0)")
    @Mapping(target = "longitude", expression = "java(attraction.getLocation() != null ? attraction.getLocation().getX() : 0.0)")
    @Mapping(target = "ratingAverage", defaultValue = "0.0")
    @Mapping(target = "reviewCount", defaultValue = "0")
    AttractionResponseDTO toResponseDTO(Attraction attraction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "guide", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "ratingAverage", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDTO(AttractionRequestDTO dto, @MappingTarget Attraction attraction);
}
