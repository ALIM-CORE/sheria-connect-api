package co.tz.sheriaconnectapi.abstractions;

import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface Query <I,O>{
    public ResponseEntity<StandardResponse<O>> execute(I input);
}
