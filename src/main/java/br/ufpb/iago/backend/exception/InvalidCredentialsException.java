package br.ufpb.iago.backend.exception;

/**
 * Lançada quando email ou senha fornecidos são inválidos durante o login.
 * Mapeada para HTTP 401 Unauthorized pelo GlobalExceptionHandler.
 * A mensagem é propositalmente genérica para evitar enumeração de usuários.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciais inválidas");
    }
}
