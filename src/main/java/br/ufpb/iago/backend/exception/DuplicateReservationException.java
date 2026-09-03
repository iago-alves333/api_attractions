package br.ufpb.iago.backend.exception;

/**
 * Lançada quando o turista já possui uma reserva pendente para a mesma atração.
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class DuplicateReservationException extends BusinessException {

    public DuplicateReservationException() {
        super("Você já possui uma reserva pendente para esta atração");
    }

    public DuplicateReservationException(String message) {
        super(message);
    }
}
