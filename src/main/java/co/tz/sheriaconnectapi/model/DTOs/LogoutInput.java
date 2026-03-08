package co.tz.sheriaconnectapi.model.DTOs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.Authentication;

@AllArgsConstructor
@Getter
public class LogoutInput {
    private Authentication auth;
    private HttpServletRequest request;
    private HttpServletResponse response;
}
