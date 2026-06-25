package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Entities.RefreshToken;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.security.Access.AccessContextResolver;
import co.tz.sheriaconnectapi.security.Access.EffectiveAccess;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import co.tz.sheriaconnectapi.security.Jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessionTokenIssuer {
    private final AuthSessionService authSessionService;
    private final ScopedAuthorityService authorityService;
    private final AccessContextResolver contextResolver;
    private final RefreshTokenRepository refreshTokenRepository;

    public SessionTokenIssuer(
            AuthSessionService authSessionService,
            ScopedAuthorityService authorityService,
            AccessContextResolver contextResolver,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.authSessionService = authSessionService;
        this.authorityService = authorityService;
        this.contextResolver = contextResolver;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public IssuedSession issue(
            User user,
            AccessContext context,
            ClientType clientType,
            boolean mfaVerified,
            HttpServletRequest request
    ) {
        EffectiveAccess access = authorityService.require(user, context);
        AuthSession session = authSessionService.create(
                user,
                context,
                clientType,
                contextResolver.audience(context),
                mfaVerified,
                request
        );
        String accessToken = JwtUtil.generateAccessToken(user, session);
        String refreshToken = JwtUtil.generateRefreshToken(user, session);

        RefreshToken entity = new RefreshToken();
        entity.setToken(refreshToken);
        entity.setUser(user);
        entity.setAuthSession(session);
        entity.setClientType(clientType);
        entity.setExpiryDate(session.getExpiresAt());
        entity.setCreatedAt(Instant.now());
        refreshTokenRepository.save(entity);

        return new IssuedSession(
                accessToken,
                refreshToken,
                new UserDTO(user, access),
                session
        );
    }
}
