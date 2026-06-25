package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.model.DTOs.LogoutInput;
import co.tz.sheriaconnectapi.repositories.AuthSessionRepository;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.security.Jwt.JwtUtil;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutService implements Command<LogoutInput, Void> {
    private final AuthSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public LogoutService(
            AuthSessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenCookieService refreshTokenCookieService
    ) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @Override
    @Transactional
    public ResponseEntity<StandardResponse<Void>> execute(LogoutInput input) {
        input.getResponse().addHeader(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookieService.clear()
        );
        String authorization = input.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (JwtUtil.isTokenValid(token)) {
                String sessionId = JwtUtil.getClaims(token).get("sid", String.class);
                sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                    session.setRevoked(true);
                    sessionRepository.save(session);
                    refreshTokenRepository.deleteAllByAuthSession_Id(session.getId());
                });
            }
        }
        SecurityContextHolder.clearContext();
        return ResponseUtil.success(null, "Logout successful", HttpStatus.OK);
    }
}
