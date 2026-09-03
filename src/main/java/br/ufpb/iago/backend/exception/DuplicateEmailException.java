package br.ufpb.iago.backend.exception;

/**
 * Lançada quando um email já está cadastrado no sistema.
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        super("Email já cadastrado");
    }

    public DuplicateEmailException(String message) {
        super(message);
    }
}
