package br.ufpb.iago.backend.exception;

/**
 * Lançada quando uma reserva não é encontrada pelo ID.
 * Herda de {@link ResourceNotFoundException} → HTTP 404.
 */
public class ReservationNotFoundException extends ResourceNotFoundException {

    public ReservationNotFoundException() {
        super("Reserva não encontrada");
    }

    public ReservationNotFoundException(String message) {
        super(message);
    }
}
