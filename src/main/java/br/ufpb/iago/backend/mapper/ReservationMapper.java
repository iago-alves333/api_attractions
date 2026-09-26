package br.ufpb.iago.backend.mapper;

import br.ufpb.iago.backend.dto.ReservationResponseDTO;
import br.ufpb.iago.backend.model.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(source = "tourist.id", target = "touristId")
    @Mapping(source = "tourist.name", target = "touristName")
    @Mapping(source = "attraction.id", target = "attractionID")
    @Mapping(source = "attraction.title", target = "attractionTitle")
    ReservationResponseDTO toResponseDTO(Reservation reservation);
}
