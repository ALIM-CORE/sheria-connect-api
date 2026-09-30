package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Entities.RefreshToken;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.AuthSessionRepository;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.security.Access.EffectiveAccess;
import co.tz.sheriaconnectapi.security.Access.AccessContextResolver;
import co.tz.sheriaconnectapi.model.DTOs.RefreshTokenRequest;
import co.tz.sheriaconnectapi.model.DTOs.RefreshInput;
import co.tz.sheriaconnectapi.exceptions.InvalidTokenException;
import org.springframework.test.util.ReflectionTestUtils;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private final RefreshTokenCookieService refreshTokenCookieService = new RefreshTokenCookieService(true, "Lax", "/");
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
        request.addHeader("X-Client-Type", "MOBILE");
        request.addHeader("X-Active-Context", "CITIZEN");
        RefreshTokenRequest body = new RefreshTokenRequest();
        ReflectionTestUtils.setField(body, "refreshToken", "old-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var service = new RefreshService(
                refreshTokenRepository,
                authSessionRepository,
                refreshTokenCookieService,
                authorityService,
                new AccessContextResolver()
        );

        var result = service.execute(new co.tz.sheriaconnectapi.model.DTOs.RefreshInput(
                request,
                response,
                body
        ));

        assertNotNull(result.getBody().getBody().get("access"));
        assertEquals("replacement-token", result.getBody().getBody().get("refresh"));
        verify(refreshTokenRepository, never()).save(rotated);
    }

    @Test
    void mixedCookieJarRefreshesOnlyTheRequestedCitizenSession() {
        var session = session(AccessContext.CITIZEN, ClientType.WEB);
        var stored = token("citizen-token", session.getUser(), session, false);
        when(refreshTokenRepository.findByToken("citizen-token")).thenReturn(java.util.Optional.of(stored));
        allowRefresh(session);
        var request = webRequest(AccessContext.CITIZEN);
        request.setCookies(new Cookie("refresh_token_staff", "staff-token"),
                new Cookie("refresh_token_citizen", "citizen-token"),
                new Cookie("refresh_token", "legacy-staff-token"));
        var response = new MockHttpServletResponse();
        var result = service().execute(new RefreshInput(request, response, null));
        assertEquals("CITIZEN", co.tz.sheriaconnectapi.security.Jwt.JwtUtil.getClaims(
                (String) result.getBody().getBody().get("access")).get("active_context", String.class));
        assertFalse(result.getBody().getBody().containsKey("refresh"));
        assertEquals(1, response.getHeaders("Set-Cookie").size());
        assertTrue(response.getHeader("Set-Cookie").startsWith("refresh_token_citizen="));
        verify(refreshTokenRepository, never()).findByToken("staff-token");
        verify(refreshTokenRepository, never()).findByToken("legacy-staff-token");
    }

    @Test
    void staffRefreshSetsOnlyStaffCookieAndRetainsMfa() {
        var session = session(AccessContext.STAFF, ClientType.WEB);
        var stored = token("staff-token", session.getUser(), session, false);
        when(refreshTokenRepository.findByToken("staff-token")).thenReturn(java.util.Optional.of(stored));
        allowRefresh(session);
        var request = webRequest(AccessContext.STAFF);
        request.setCookies(new Cookie("refresh_token_staff", "staff-token"),
                new Cookie("refresh_token_citizen", "citizen-token"));
        var response = new MockHttpServletResponse();
        var result = service().execute(new RefreshInput(request, response, null));
        assertTrue(response.getHeader("Set-Cookie").startsWith("refresh_token_staff="));
        assertEquals(true, co.tz.sheriaconnectapi.security.Jwt.JwtUtil.getClaims(
                (String) result.getBody().getBody().get("access")).get("mfa_verified", Boolean.class));
        verify(refreshTokenRepository, never()).findByToken("citizen-token");
    }

    @Test
    void legacyCookieMigratesOnlyAfterItsSessionMatches() {
        var session = session(AccessContext.STAFF, ClientType.WEB);
        when(refreshTokenRepository.findByToken("legacy")).thenReturn(
                java.util.Optional.of(token("legacy", session.getUser(), session, false)));
        allowRefresh(session);
        var request = webRequest(AccessContext.STAFF);
        request.setCookies(new Cookie("refresh_token", "legacy"));
        var response = new MockHttpServletResponse();
        service().execute(new RefreshInput(request, response, null));
        assertEquals(2, response.getHeaders("Set-Cookie").size());
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(value -> value.startsWith("refresh_token_staff=")));
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(value ->
                value.startsWith("refresh_token=;") && value.contains("Max-Age=0")));
    }

    @Test
    void wrongContextLegacyCookieCannotRefreshOrRevokeTheOtherSession() {
        var session = session(AccessContext.STAFF, ClientType.WEB);
        var stored = token("legacy", session.getUser(), session, false);
        when(refreshTokenRepository.findByToken("legacy")).thenReturn(java.util.Optional.of(stored));
        var request = webRequest(AccessContext.CITIZEN);
        request.setCookies(new Cookie("refresh_token", "legacy"));
        assertThrows(InvalidTokenException.class,
                () -> service().execute(new RefreshInput(request, new MockHttpServletResponse(), null)));
        assertFalse(stored.isRevoked());
        verify(refreshTokenRepository, never()).save(any());
        verify(authSessionRepository, never()).save(any());
        verifyNoInteractions(authorityService);
    }

    @Test
    void webTokenCannotBeRefreshedThroughMobileBody() {
        var session = session(AccessContext.CITIZEN, ClientType.WEB);
        when(refreshTokenRepository.findByToken("web-token")).thenReturn(
                java.util.Optional.of(token("web-token", session.getUser(), session, false)));
        var request = new MockHttpServletRequest();
        request.addHeader("X-Client-Type", "MOBILE");
        request.addHeader("X-Active-Context", "CITIZEN");
        var body = new RefreshTokenRequest();
        ReflectionTestUtils.setField(body, "refreshToken", "web-token");
        assertThrows(InvalidTokenException.class,
                () -> service().execute(new RefreshInput(request, new MockHttpServletResponse(), body)));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void mobileRequestsIgnoreBrowserCookies() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Client-Type", "MOBILE");
        request.addHeader("X-Active-Context", "CITIZEN");
        request.setCookies(new Cookie("refresh_token_citizen", "web-token"),
                new Cookie("refresh_token", "legacy"));
        assertThrows(InvalidTokenException.class,
                () -> service().execute(new RefreshInput(request, new MockHttpServletResponse(), null)));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void revokedReplacementCannotCrossContextsWithinReuseGrace() {
        var session = session(AccessContext.STAFF, ClientType.WEB);
        var old = token("old", session.getUser(), session, true);
        old.setRevokedAt(Instant.now().minusSeconds(3));
        old.setReplacedByToken(token("replacement", session.getUser(), session, false));
        when(refreshTokenRepository.findByToken("old")).thenReturn(java.util.Optional.of(old));
        var request = webRequest(AccessContext.CITIZEN);
        request.setCookies(new Cookie("refresh_token_citizen", "old"));
        assertThrows(InvalidTokenException.class,
                () -> service().execute(new RefreshInput(request, new MockHttpServletResponse(), null)));
        verifyNoInteractions(authorityService);
    }

    private RefreshService service() {
        return new RefreshService(refreshTokenRepository, authSessionRepository,
                refreshTokenCookieService, authorityService, new AccessContextResolver());
    }

    private MockHttpServletRequest webRequest(AccessContext context) {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Client-Type", "WEB");
        request.addHeader("X-Active-Context", context.name());
        return request;
    }

    private AuthSession session(AccessContext context, ClientType clientType) {
        var user = new User();
        user.setId(3L);
        user.setName("Shared identity");
        user.setEmail("shared@example.test");
        user.setActive(true);
        user.setLocked(false);
        var session = new AuthSession();
        session.setId(99L);
        session.setUser(user);
        session.setActiveContext(context);
        session.setClientType(clientType);
        session.setAudience(new AccessContextResolver().audience(context));
        session.setMfaVerified(context == AccessContext.STAFF);
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        return session;
    }

    private void allowRefresh(AuthSession session) {
        when(authorityService.resolve(session.getUser(), session.getActiveContext()))
                .thenReturn(new EffectiveAccess(session.getActiveContext(),
                        List.of(new Role(session.getActiveContext().name())), Set.of()));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
        token.setClientType(session.getClientType());
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        token.setRevoked(revoked);
        return token;
    }
}
