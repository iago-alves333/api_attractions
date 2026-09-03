package br.ufpb.iago.backend.exception;

/**
 * Lançada quando se tenta alterar o role de um usuário ADMIN.
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class AdminRoleChangeException extends BusinessException {

    public AdminRoleChangeException() {
        super("Não é possível alterar o role de um ADMIN");
    }

    public AdminRoleChangeException(String message) {
        super(message);
    }
}
