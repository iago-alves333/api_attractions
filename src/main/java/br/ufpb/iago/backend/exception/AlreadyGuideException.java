package br.ufpb.iago.backend.exception;

/**
 * Lançada quando se tenta promover um usuário que já possui o role GUIDE.
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class AlreadyGuideException extends BusinessException {

    public AlreadyGuideException() {
        super("Este usuário já é um GUIDE");
    }

    public AlreadyGuideException(String message) {
        super(message);
    }
}
