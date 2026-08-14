package br.ufpb.iago.backend.exception;

/**
 * Lançada quando uma regra de negócio é violada (ex: email duplicado, sem vagas).
 * Mapeada para HTTP 409 Conflict pelo GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
