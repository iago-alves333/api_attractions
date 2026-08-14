package br.ufpb.iago.backend.exception;

/**
 * Lançada quando um recurso não é encontrado no banco de dados.
 * Mapeada para HTTP 404 Not Found pelo GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
