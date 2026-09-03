package br.ufpb.iago.backend.exception;

/**
 * Lançada quando uma transição de status de reserva é inválida.
 * Ex: tentar confirmar uma reserva que já foi cancelada.
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class InvalidReservationStateException extends BusinessException {

    public InvalidReservationStateException(String message) {
        super(message);
    }
}
