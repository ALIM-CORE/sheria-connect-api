package co.tz.sheriaconnectapi.services.AuthServices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenCookieService {

    private final boolean secure;
    private final String sameSite;
    private final String path;

    public RefreshTokenCookieService(
            @Value("${app.auth.refresh-cookie.secure}") boolean secure,
            @Value("${app.auth.refresh-cookie.same-site}") String sameSite,
            @Value("${app.auth.refresh-cookie.path}") String path
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.path = path;
    }

    public String create(String token, Duration maxAge) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build()
                .toString();
    }

    public String clear() {
        return create("", Duration.ZERO);
    }
}
