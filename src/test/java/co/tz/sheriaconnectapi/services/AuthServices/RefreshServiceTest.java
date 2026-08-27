package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Entities.RefreshToken;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.AuthSessionRepository;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.security.Access.EffectiveAccess;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshServiceTest {
    static {
        System.setProperty(
                "JWT_SECRET",
                "test-only-jwt-secret-that-is-long-enough-for-hs512-signing-1234567890"
        );
    }

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AuthSessionRepository authSessionRepository;
    @Mock
    private RefreshTokenCookieService refreshTokenCookieService;
    @Mock
    private ScopedAuthorityService authorityService;

    @Test
    void recentlyRotatedTokenReturnsExistingReplacementForSameSession() {
        User user = new User();
        user.setId(3L);
        user.setEmail("citizen@sheriaconnect.co.tz");
        user.setName("Citizen");
        user.setActive(true);
        user.setLocked(false);

        AuthSession session = new AuthSession();
        session.setId(99L);
        session.setUser(user);
        session.setActiveContext(AccessContext.CITIZEN);
        session.setClientType(ClientType.MOBILE);
        session.setAudience("sheria-connect-mobile");
        session.setExpiresAt(Instant.now().plusSeconds(3600));

        RefreshToken replacement = token("replacement-token", user, session, false);
        replacement.setId(2L);

        RefreshToken rotated = token("old-token", user, session, true);
        rotated.setId(1L);
        rotated.setRevokedAt(Instant.now().minusSeconds(3));
        rotated.setReplacedByToken(replacement);

        Role role = new Role("CITIZEN");
        when(refreshTokenRepository.findByToken("old-token")).thenReturn(java.util.Optional.of(rotated));
        when(authorityService.resolve(user, AccessContext.CITIZEN))
                .thenReturn(new EffectiveAccess(AccessContext.CITIZEN, List.of(role), Set.of()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "old-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        var service = new RefreshService(
                refreshTokenRepository,
                authSessionRepository,
                refreshTokenCookieService,
                authorityService
        );

        var result = service.execute(new co.tz.sheriaconnectapi.model.DTOs.RefreshInput(
                request,
                response,
                null
        ));

        assertNotNull(result.getBody().getBody().get("access"));
        assertEquals("replacement-token", result.getBody().getBody().get("refresh"));
        verify(refreshTokenRepository, never()).save(rotated);
    }

    private RefreshToken token(
            String value,
            User user,
            AuthSession session,
            boolean revoked
    ) {
        RefreshToken token = new RefreshToken();
        token.setToken(value);
        token.setUser(user);
        token.setAuthSession(session);
        token.setClientType(ClientType.MOBILE);
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        token.setRevoked(revoked);
        return token;
    }
}
