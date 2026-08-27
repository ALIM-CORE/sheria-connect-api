package co.tz.sheriaconnectapi.exceptions;

public abstract class DomainException extends RuntimeException {
    private final ErrorMessages error;

    protected DomainException(ErrorMessages error) {
        super(error.getMessage());
        this.error = error;
    }

    protected DomainException(ErrorMessages error, String message) {
        super(message == null || message.isBlank() ? error.getMessage() : message);
        this.error = error;
    }

    protected DomainException(ErrorMessages error, Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
    }

    public ErrorMessages getError() {
        return error;
    }
}
