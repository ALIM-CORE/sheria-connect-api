package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.AuthSessionRepository;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthSessionService {
    private final AuthSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthSessionService(
            AuthSessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public AuthSession create(
            User user,
            AccessContext context,
            ClientType clientType,
            String audience,
            boolean mfaVerified,
            HttpServletRequest request
    ) {
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setActiveContext(context);
        session.setClientType(clientType);
        session.setAudience(audience);
        session.setMfaVerified(mfaVerified);
        session.setIpAddress(clientIp(request));
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setExpiresAt(Instant.now().plus(
                clientType == ClientType.WEB ? 7 : 30,
                ChronoUnit.DAYS
        ));
        return sessionRepository.save(session);
    }

    @Transactional
    public void revoke(AuthSession session) {
        session.setRevoked(true);
        sessionRepository.save(session);
        refreshTokenRepository.deleteAllByAuthSession_Id(session.getId());
    }

    @Transactional
    public void revokeContext(Long userId, AccessContext context) {
        sessionRepository.revokeByUserIdAndContext(userId, context);
        refreshTokenRepository.revokeAllByUserIdAndContext(userId, context);
    }

    @Transactional
    public void revokeAll(Long userId) {
        sessionRepository.revokeAllByUserId(userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    public String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
