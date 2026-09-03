package br.ufpb.iago.backend.exception;

/**
 * Lançada quando um guia não é encontrado pelo ID.
 * Herda de {@link ResourceNotFoundException} → HTTP 404.
 */
public class GuideNotFoundException extends ResourceNotFoundException {

    public GuideNotFoundException() {
        super("Guia não encontrado");
    }

    public GuideNotFoundException(String message) {
        super(message);
    }
}
