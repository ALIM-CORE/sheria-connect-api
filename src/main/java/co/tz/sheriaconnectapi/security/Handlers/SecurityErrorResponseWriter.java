package co.tz.sheriaconnectapi.security.Handlers;

import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SecurityErrorResponseWriter {

    private final JsonMapper jsonMapper;

    public SecurityErrorResponseWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void write(HttpServletResponse response, int status, String message)
            throws IOException {
        write(response, status, message, null);
    }

    public void write(HttpServletResponse response, int status, ErrorMessages error)
            throws IOException {
        write(response, status, error.getMessage(), error.name());
    }

    private void write(
            HttpServletResponse response,
            int status,
            String message,
            String code
    )
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(
                response.getOutputStream(),
                new StandardResponse<>(false, null, null, message, code)
        );
    }
}
