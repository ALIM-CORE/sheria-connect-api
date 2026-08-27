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
import co.tz.sheriaconnectapi.security.Access.EffectiveAccess;
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
    private static final Duration ROTATED_TOKEN_REUSE_GRACE = Duration.ofSeconds(30);

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
            throw new InvalidTokenException(ErrorMessages.MISSING_TOKEN);
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException(ErrorMessages.INVALID_TOKEN));
        Instant now = Instant.now();
        if (storedToken.isRevoked()) {
            return reuseRecentlyRotatedToken(input, storedToken, now);
        }

        RefreshContext context = validateUsableToken(storedToken, now);
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(now);

        context.session().setLastActivityAt(now);
        authSessionRepository.save(context.session());

        String newRefreshToken = JwtUtil.generateRefreshToken(context.user(), context.session());
        RefreshToken replacement = new RefreshToken();
        replacement.setToken(newRefreshToken);
        replacement.setUser(context.user());
        replacement.setAuthSession(context.session());
        replacement.setClientType(context.session().getClientType());
        replacement.setExpiryDate(context.session().getExpiresAt());
        replacement = refreshTokenRepository.save(replacement);

        storedToken.setReplacedByToken(replacement);
        refreshTokenRepository.save(storedToken);

        return refreshedResponse(input, context, newRefreshToken);
    }

    private ResponseEntity<StandardResponse<Map<String, Object>>> reuseRecentlyRotatedToken(
            RefreshInput input,
            RefreshToken storedToken,
            Instant now
    ) {
        RefreshToken replacement = storedToken.getReplacedByToken();
        if (!isWithinReuseGrace(storedToken, replacement, now)) {
            throw new InvalidTokenException(ErrorMessages.INVALID_TOKEN);
        }

        RefreshContext context = validateUsableToken(replacement, now);
        context.session().setLastActivityAt(now);
        authSessionRepository.save(context.session());
        return refreshedResponse(input, context, replacement.getToken());
    }

    private boolean isWithinReuseGrace(
            RefreshToken storedToken,
            RefreshToken replacement,
            Instant now
    ) {
        return replacement != null
                && storedToken.getRevokedAt() != null
                && !storedToken.getRevokedAt().isBefore(now.minus(ROTATED_TOKEN_REUSE_GRACE))
                && storedToken.getExpiryDate() != null
                && !storedToken.getExpiryDate().isBefore(now)
                && replacement.getAuthSession() != null
                && storedToken.getAuthSession() != null
                && replacement.getAuthSession().getId().equals(storedToken.getAuthSession().getId());
    }

    private RefreshContext validateUsableToken(RefreshToken token, Instant now) {
        AuthSession session = token.getAuthSession();
        if (token.isRevoked()
                || token.getExpiryDate() == null
                || token.getExpiryDate().isBefore(now)
                || session == null
                || session.isRevoked()
                || session.getExpiresAt().isBefore(now)) {
            throw new InvalidTokenException(ErrorMessages.INVALID_TOKEN);
        }

        User user = token.getUser();
        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getLocked())) {
            session.setRevoked(true);
            authSessionRepository.save(session);
            revoke(token);
            throw new InvalidTokenException(ErrorMessages.INVALID_TOKEN);
        }
        if (session.getActiveContext() == co.tz.sheriaconnectapi.model.Enums.AccessContext.STAFF
                && !session.isMfaVerified()) {
            throw new WebPortalAccessDeniedException();
        }

        var effectiveAccess = authorityService.resolve(user, session.getActiveContext());
        if (effectiveAccess.roles().isEmpty()) {
            session.setRevoked(true);
            authSessionRepository.save(session);
            revoke(token);
            throw new InvalidTokenException(ErrorMessages.INVALID_TOKEN);
        }

        return new RefreshContext(user, session, effectiveAccess);
    }

    private void revoke(RefreshToken token) {
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    private ResponseEntity<StandardResponse<Map<String, Object>>> refreshedResponse(
            RefreshInput input,
            RefreshContext context,
            String refreshToken
    ) {
        String newAccessToken = JwtUtil.generateAccessToken(context.user(), context.session());
        if (context.session().getClientType() == ClientType.WEB) {
            input.getResponse().addHeader(
                    HttpHeaders.SET_COOKIE,
                    refreshTokenCookieService.create(refreshToken, Duration.ofDays(7))
            );
        }

        Map<String, Object> body = new HashMap<>();
        body.put("access", newAccessToken);
        body.put("user", new UserDTO(context.user(), context.effectiveAccess()));
        if (context.session().getClientType() == ClientType.MOBILE) {
            body.put("refresh", refreshToken);
        }
        return ResponseUtil.success(body, "Token refreshed successfully", HttpStatus.OK);
    }

    private record RefreshContext(
            User user,
            AuthSession session,
            EffectiveAccess effectiveAccess
    ) {
    }
}
