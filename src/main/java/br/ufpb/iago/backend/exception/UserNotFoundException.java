package br.ufpb.iago.backend.exception;

/**
 * Lançada quando um usuário não é encontrado pelo ID ou email.
 * Herda de {@link ResourceNotFoundException} → HTTP 404.
 */
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException() {
        super("Usuário não encontrado");
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
