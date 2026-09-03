package br.ufpb.iago.backend.exception;

/**
 * Lançada quando um guia tenta reservar sua própria atração.
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class SelfReservationException extends BusinessException {

    public SelfReservationException() {
        super("O guia não pode reservar sua própria atração");
    }

    public SelfReservationException(String message) {
        super(message);
    }
}
