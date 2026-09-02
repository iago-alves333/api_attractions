package br.ufpb.iago.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequestDTO {

    @NotBlank(message = "O refresh token é obrigatório")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
