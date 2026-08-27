package co.tz.sheriaconnectapi.utils;

import co.tz.sheriaconnectapi.exceptions.DomainException;
import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtil {

    private ResponseUtil() {}

    public static <T> ResponseEntity<StandardResponse<T>> success(
            T body,
            String message,
            HttpStatus status
    ) {
        return new ResponseEntity<>(
                new StandardResponse<>(true, message, body, null),
                status
        );
    }

    public static ResponseEntity<StandardResponse<Void>> error(
            String errorMessage,
            HttpStatus status
    ) {
        return new ResponseEntity<>(
                new StandardResponse<>(false, null, null, errorMessage),
                status
        );
    }

    public static ResponseEntity<StandardResponse<Void>> error(
            ErrorMessages error,
            HttpStatus status
    ) {
        return error(error.getMessage(), error, status);
    }

    public static ResponseEntity<StandardResponse<Void>> error(
            DomainException exception,
            HttpStatus status
    ) {
        return error(exception.getMessage(), exception.getError(), status);
    }

    public static ResponseEntity<StandardResponse<Void>> error(
            String errorMessage,
            ErrorMessages error,
            HttpStatus status
    ) {
        return new ResponseEntity<>(
                new StandardResponse<>(
                        false,
                        null,
                        null,
                        errorMessage,
                        error.name()
                ),
                status
        );
    }
}

