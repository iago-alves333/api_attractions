package br.ufpb.iago.backend.exception;

/**
 * Lançada quando um review não é encontrado pelo ID.
 * Herda de {@link ResourceNotFoundException} → HTTP 404.
 */
public class ReviewNotFoundException extends ResourceNotFoundException {

    public ReviewNotFoundException() {
        super("Review não encontrado");
    }

    public ReviewNotFoundException(String message) {
        super(message);
    }
}
