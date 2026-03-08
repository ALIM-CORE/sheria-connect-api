package co.tz.sheriaconnectapi.abstractions;

import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface Command<I, O> {
    ResponseEntity<StandardResponse<O>> execute(I input);
}
