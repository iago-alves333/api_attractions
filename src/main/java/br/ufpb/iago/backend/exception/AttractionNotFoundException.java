package br.ufpb.iago.backend.exception;

/**
 * Lançada quando uma atração turística não é encontrada pelo ID.
 * Herda de {@link ResourceNotFoundException} → HTTP 404.
 */
public class AttractionNotFoundException extends ResourceNotFoundException {

    public AttractionNotFoundException() {
        super("Atração não encontrada");
    }

    public AttractionNotFoundException(String message) {
        super(message);
    }
}
