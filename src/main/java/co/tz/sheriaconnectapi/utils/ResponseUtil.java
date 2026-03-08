package co.tz.sheriaconnectapi.utils;

import org.springframework.http.HttpHeaders;
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
}

