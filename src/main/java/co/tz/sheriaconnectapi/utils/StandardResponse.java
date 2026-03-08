package co.tz.sheriaconnectapi.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class StandardResponse<T> {

    private final boolean success;
    private final String message;
    private final T body;
    private final String error;
    private final Instant timestamp;

    public StandardResponse(
            boolean success,
            String message,
            T body,
            String error
    ) {
        this.success = success;
        this.message = message;
        this.body = body;
        this.error = error;
        this.timestamp = Instant.now();
    }
}

