package co.tz.sheriaconnectapi.model.DTOs;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import co.tz.sheriaconnectapi.model.Entities.User;

@Getter
@AllArgsConstructor
public class RegisterInput {
    private User user;
    private HttpServletRequest request;

    public String getFrontendBaseUrl() {
        // Example: https://app.sheriaconnect.co.tz
        return request.getHeader("X-Frontend-Base-Url");
    }
}
