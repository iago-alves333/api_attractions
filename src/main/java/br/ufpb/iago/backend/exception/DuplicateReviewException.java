package br.ufpb.iago.backend.exception;

/**
 * Lançada quando o turista tenta avaliar uma atração que ele já avaliou.
 * Herda de {@link BusinessException} → HTTP 409.
 */
public class DuplicateReviewException extends BusinessException {

    public DuplicateReviewException() {
        super("Você já avaliou esta atração");
    }

    public DuplicateReviewException(String message) {
        super(message);
    }
}
