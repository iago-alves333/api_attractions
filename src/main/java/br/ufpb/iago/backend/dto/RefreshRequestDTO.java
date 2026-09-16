package br.ufpb.iago.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequestDTO implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "O refresh token é obrigatório")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
