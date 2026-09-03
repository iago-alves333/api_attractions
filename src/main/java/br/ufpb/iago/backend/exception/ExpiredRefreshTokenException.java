package br.ufpb.iago.backend.exception;

/**
 * Lançada quando um refresh token existe mas já expirou.
 * Herda de {@link InvalidRefreshTokenException} → HTTP 409.
 */
public class ExpiredRefreshTokenException extends InvalidRefreshTokenException {

    public ExpiredRefreshTokenException() {
        super("Refresh token expirado");
    }

    public ExpiredRefreshTokenException(String message) {
        super(message);
    }
}
