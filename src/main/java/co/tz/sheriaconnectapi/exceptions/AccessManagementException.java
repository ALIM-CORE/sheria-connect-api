package co.tz.sheriaconnectapi.exceptions;

import org.springframework.http.HttpStatus;

public class AccessManagementException extends RuntimeException {
    private final HttpStatus status;

    public AccessManagementException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
