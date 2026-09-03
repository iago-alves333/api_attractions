package br.ufpb.iago.backend.exception;

/**
 * Lançada quando uma atração não tem vagas disponíveis para reserva.
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class NoAvailableSpotsException extends BusinessException {

    public NoAvailableSpotsException() {
        super("Não há vagas disponíveis para esta atração");
    }

    public NoAvailableSpotsException(String message) {
        super(message);
    }
}
