package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

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

    public String create(AccessContext context, String token, Duration maxAge) {
        return cookie(name(context), token, maxAge);
    }

    public String name(AccessContext context) {
        return "refresh_token_" + context.name().toLowerCase(Locale.ROOT);
    }

    public String read(HttpServletRequest request, AccessContext context) {
        return readCookie(request, name(context));
    }

    public String readLegacy(HttpServletRequest request) {
        return readCookie(request, "refresh_token");
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String cookie(String name, String token, Duration maxAge) {
        return ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build()
                .toString();
    }

    public String clear(AccessContext context) {
        return create(context, "", Duration.ZERO);
    }

    public String clearLegacy() {
        return cookie("refresh_token", "", Duration.ZERO);
    }
}
