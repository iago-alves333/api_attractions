package br.ufpb.iago.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class AttractionRequestDTO {

    @NotNull(message = "O título é obrigatório")
    @Size(max = 150, message = "O título não pode ter mais de 150 caracteres")
    private String title;

    @Size(max = 2000, message = "A descrição não pode ter mais de 2000 caracteres")
    private String description;

    @NotNull(message = "O preço é obrigatório")
    @PositiveOrZero(message = "O preço não pode ser negativo")
    private BigDecimal price;

    @NotNull(message = "A quantidade de vagas é obrigatória")
    @PositiveOrZero(message = "A quantidade de vagas não pode ser negativa")
    private Integer availableSpots;

    @NotNull(message = "A latitude é obrigatória")
    @DecimalMin(value = "-90.0", message = "Latitude inválida")
    @DecimalMax(value = "90.0", message = "Latitude inválida")
    private Double latitude;

    @NotNull(message = "A longitude é obrigatória")
    @DecimalMin(value = "-180.0", message = "Longitude inválida")
    @DecimalMax(value = "180.0", message = "Longitude inválida")
    private Double longitude;

    // getters e setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getAvailableSpots() {
        return availableSpots;
    }

    public void setAvailableSpots(Integer availableSpots) {
        this.availableSpots = availableSpots;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}