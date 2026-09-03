package br.ufpb.iago.backend.exception;

/**
 * Lançada quando um refresh token é inválido (não encontrado no banco).
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super("Refresh token inválido");
    }

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
