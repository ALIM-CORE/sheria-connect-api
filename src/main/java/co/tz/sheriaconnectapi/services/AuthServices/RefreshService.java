package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
import co.tz.sheriaconnectapi.exceptions.InvalidTokenException;
import co.tz.sheriaconnectapi.exceptions.WebPortalAccessDeniedException;
import co.tz.sheriaconnectapi.model.DTOs.RefreshInput;
import co.tz.sheriaconnectapi.model.DTOs.RefreshTokenRequest;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Entities.RefreshToken;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.AuthSessionRepository;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import co.tz.sheriaconnectapi.security.Jwt.JwtUtil;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class RefreshService implements Command<RefreshInput, Map<String, Object>> {
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthSessionRepository authSessionRepository;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final ScopedAuthorityService authorityService;

    public RefreshService(
            RefreshTokenRepository refreshTokenRepository,
            AuthSessionRepository authSessionRepository,
            RefreshTokenCookieService refreshTokenCookieService,
            ScopedAuthorityService authorityService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authSessionRepository = authSessionRepository;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.authorityService = authorityService;
    }

    private String extractRefreshToken(HttpServletRequest request, RefreshTokenRequest body) {
        String cookieToken = Arrays.stream(
                        Optional.ofNullable(request.getCookies()).orElse(new Cookie[0])
                )
                .filter(cookie -> cookie.getName().equals("refresh_token"))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        if (cookieToken != null) {
            return cookieToken;
        }
        return body == null ? null : body.getRefreshToken();
    }

    @Override
    @Transactional
    public ResponseEntity<StandardResponse<Map<String, Object>>> execute(RefreshInput input) {
        String tokenValue = extractRefreshToken(input.getRequest(), input.getBody());
        if (tokenValue == null) {
            throw new InvalidTokenException(ErrorMessages.MISSING_TOKEN.getMessage());
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException(ErrorMessages.INVALID_TOKEN.getMessage()));
        AuthSession session = storedToken.getAuthSession();
        if (storedToken.isRevoked()
                || storedToken.getExpiryDate().isBefore(Instant.now())
                || session == null
                || session.isRevoked()
                || session.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException(ErrorMessages.INVALID_TOKEN.getMessage());
        }

        User user = storedToken.getUser();
        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getLocked())) {
            session.setRevoked(true);
            authSessionRepository.save(session);
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new InvalidTokenException(ErrorMessages.INVALID_TOKEN.getMessage());
        }
        if (session.getActiveContext() == co.tz.sheriaconnectapi.model.Enums.AccessContext.STAFF
                && !session.isMfaVerified()) {
            throw new WebPortalAccessDeniedException();
        }

        var effectiveAccess = authorityService.resolve(user, session.getActiveContext());
        if (effectiveAccess.roles().isEmpty()) {
            session.setRevoked(true);
            authSessionRepository.save(session);
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new InvalidTokenException(ErrorMessages.INVALID_TOKEN.getMessage());
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
        session.setLastActivityAt(Instant.now());
        authSessionRepository.save(session);

        String newAccessToken = JwtUtil.generateAccessToken(user, session);
        String newRefreshToken = JwtUtil.generateRefreshToken(user, session);
        RefreshToken replacement = new RefreshToken();
        replacement.setToken(newRefreshToken);
        replacement.setUser(user);
        replacement.setAuthSession(session);
        replacement.setClientType(session.getClientType());
        replacement.setExpiryDate(session.getExpiresAt());
        refreshTokenRepository.save(replacement);

        if (session.getClientType() == ClientType.WEB) {
            input.getResponse().addHeader(
                    HttpHeaders.SET_COOKIE,
                    refreshTokenCookieService.create(newRefreshToken, Duration.ofDays(7))
            );
        }

        Map<String, Object> body = new HashMap<>();
        body.put("access", newAccessToken);
        body.put("user", new UserDTO(user, effectiveAccess));
        if (session.getClientType() == ClientType.MOBILE) {
            body.put("refresh", newRefreshToken);
        }
        return ResponseUtil.success(body, "Token refreshed successfully", HttpStatus.OK);
    }
}
