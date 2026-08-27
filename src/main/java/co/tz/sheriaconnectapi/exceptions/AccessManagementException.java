package co.tz.sheriaconnectapi.exceptions;

import org.springframework.http.HttpStatus;

public class AccessManagementException extends DomainException {
    private final HttpStatus status;

    public AccessManagementException(String message, HttpStatus status) {
        super(ErrorMessages.ACCESS_MANAGEMENT_ERROR, message);
        this.status = status;
    }

    public AccessManagementException(ErrorMessages error, HttpStatus status) {
        super(error);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
