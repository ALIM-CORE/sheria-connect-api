package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.model.DTOs.LogoutInput;
import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.exceptions.InvalidTokenException;
import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
import co.tz.sheriaconnectapi.security.Access.AccessContextResolver;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
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
    private final AccessContextResolver accessContextResolver;

    public LogoutService(
            AuthSessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenCookieService refreshTokenCookieService,
            AccessContextResolver accessContextResolver
    ) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.accessContextResolver = accessContextResolver;
    }

    @Override
    @Transactional
    public ResponseEntity<StandardResponse<Void>> execute(LogoutInput input) {
        ClientType clientType = accessContextResolver.clientType(input.getRequest());
        AccessContext webContext = clientType == ClientType.WEB
                ? accessContextResolver.context(input.getRequest(), clientType, null)
                : null;
        AuthSession session = null;
        String authorization = input.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (JwtUtil.isTokenValid(token)) {
                String sessionId = JwtUtil.getClaims(token).get("sid", String.class);
                session = sessionRepository.findBySessionId(sessionId).orElse(null);
                if (session != null && !accessContextResolver.matchesSession(input.getRequest(), session)) {
                    throw new InvalidTokenException(ErrorMessages.INVALID_TOKEN);
                }
            }
        }
        boolean usedLegacyCookie = false;
        if (clientType == ClientType.WEB) {
            String cookieToken = refreshTokenCookieService.read(input.getRequest(), webContext);
            if (cookieToken == null) {
                cookieToken = refreshTokenCookieService.readLegacy(input.getRequest());
                usedLegacyCookie = cookieToken != null;
            }
            if (cookieToken != null) {
                AuthSession cookieSession = refreshTokenRepository.findByToken(cookieToken)
                        .map(token -> token.getAuthSession()).orElse(null);
                boolean matchesContext = accessContextResolver.matchesSession(input.getRequest(), cookieSession);
                usedLegacyCookie = usedLegacyCookie && matchesContext;
                // Expired/missing access tokens must not leave a renewable web session behind.
                if (session == null && matchesContext) {
                    session = cookieSession;
                }
            }
            input.getResponse().addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clear(webContext));
            if (usedLegacyCookie) {
                input.getResponse().addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clearLegacy());
            }
        }
        if (session != null) {
            session.setRevoked(true);
            sessionRepository.save(session);
            refreshTokenRepository.deleteAllByAuthSession_Id(session.getId());
        }
        SecurityContextHolder.clearContext();
        return ResponseUtil.success(null, "Logout successful", HttpStatus.OK);
    }
}
