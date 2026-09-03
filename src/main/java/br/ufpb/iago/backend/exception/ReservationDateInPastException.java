package br.ufpb.iago.backend.exception;

/**
 * Lançada quando a data de uma reserva está no passado.
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class ReservationDateInPastException extends BusinessException {

    public ReservationDateInPastException() {
        super("A data da reserva deve ser no futuro");
    }

    public ReservationDateInPastException(String message) {
        super(message);
    }
}
