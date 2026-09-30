package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.exceptions.InvalidTokenException;
import co.tz.sheriaconnectapi.model.DTOs.LogoutInput;
import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Entities.RefreshToken;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.AuthSessionRepository;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.security.Access.AccessContextResolver;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import co.tz.sheriaconnectapi.security.Jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {
    static {
        System.setProperty("JWT_SECRET", "test-only-jwt-secret-that-is-long-enough-for-hs512-signing-1234567890");
    }
    @Mock private AuthSessionRepository sessions;
    @Mock private RefreshTokenRepository tokens;
    private final RefreshTokenCookieService cookies = new RefreshTokenCookieService(true, "Lax", "/");

    @Test
    void staffLogoutRevokesOnlyStaffAndClearsOnlyStaffCookie() {
        var session = session(AccessContext.STAFF, ClientType.WEB);
        when(sessions.findBySessionId(session.getSessionId())).thenReturn(Optional.of(session));
        var request = request(AccessContext.STAFF, ClientType.WEB);
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateAccessToken(session.getUser(), session));
        request.setCookies(new Cookie("refresh_token_citizen", "unrelated-citizen"));
        var response = new MockHttpServletResponse();
        service().execute(new LogoutInput(null, request, response));
        assertTrue(session.isRevoked());
        verify(tokens).deleteAllByAuthSession_Id(session.getId());
        verify(tokens, never()).findByToken("unrelated-citizen");
        assertEquals(1, response.getHeaders("Set-Cookie").size());
        assertTrue(response.getHeader("Set-Cookie").startsWith("refresh_token_staff=;"));
    }

    @Test
    void missingAccessTokenStillRevokesTheSelectedCookieSession() {
        var session = session(AccessContext.CITIZEN, ClientType.WEB);
        var token = new RefreshToken();
        token.setAuthSession(session);
        when(tokens.findByToken("citizen")).thenReturn(Optional.of(token));
        var request = request(AccessContext.CITIZEN, ClientType.WEB);
        request.addHeader("Authorization", "Bearer expired-or-invalid-access-token");
        request.setCookies(new Cookie("refresh_token_citizen", "citizen"),
                new Cookie("refresh_token_staff", "staff"));
        var response = new MockHttpServletResponse();
        service().execute(new LogoutInput(null, request, response));
        assertTrue(session.isRevoked());
        assertTrue(response.getHeader("Set-Cookie").startsWith("refresh_token_citizen=;"));
        verify(tokens, never()).findByToken("staff");
    }

    @Test
    void unrelatedLegacyCookieIsNotClearedOrRevoked() {
        var staff = session(AccessContext.STAFF, ClientType.WEB);
        var token = new RefreshToken();
        token.setAuthSession(staff);
        when(tokens.findByToken("legacy-staff")).thenReturn(Optional.of(token));
        var request = request(AccessContext.CITIZEN, ClientType.WEB);
        request.setCookies(new Cookie("refresh_token", "legacy-staff"));
        var response = new MockHttpServletResponse();
        service().execute(new LogoutInput(null, request, response));
        assertFalse(staff.isRevoked());
        assertEquals(1, response.getHeaders("Set-Cookie").size());
        assertTrue(response.getHeader("Set-Cookie").startsWith("refresh_token_citizen=;"));
        verify(sessions, never()).save(any());
        verify(tokens, never()).deleteAllByAuthSession_Id(any());
    }

    @Test
    void mobileLogoutDoesNotTouchWebCookies() {
        var session = session(AccessContext.PROVIDER, ClientType.MOBILE);
        when(sessions.findBySessionId(session.getSessionId())).thenReturn(Optional.of(session));
        var request = request(AccessContext.PROVIDER, ClientType.MOBILE);
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateAccessToken(session.getUser(), session));
        request.setCookies(new Cookie("refresh_token_staff", "staff"));
        var response = new MockHttpServletResponse();
        service().execute(new LogoutInput(null, request, response));
        assertTrue(session.isRevoked());
        assertTrue(response.getHeaders("Set-Cookie").isEmpty());
        verify(tokens, never()).findByToken(any());
    }

    @Test
    void mismatchedBearerContextCannotLogOutAnotherContext() {
        var session = session(AccessContext.STAFF, ClientType.WEB);
        when(sessions.findBySessionId(session.getSessionId())).thenReturn(Optional.of(session));
        var request = request(AccessContext.CITIZEN, ClientType.WEB);
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateAccessToken(session.getUser(), session));
        var response = new MockHttpServletResponse();
        assertThrows(InvalidTokenException.class, () -> service().execute(new LogoutInput(null, request, response)));
        assertFalse(session.isRevoked());
        assertTrue(response.getHeaders("Set-Cookie").isEmpty());
        verify(sessions, never()).save(any());
        verifyNoInteractions(tokens);
    }

    private LogoutService service() {
        return new LogoutService(sessions, tokens, cookies, new AccessContextResolver());
    }

    private MockHttpServletRequest request(AccessContext context, ClientType type) {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Client-Type", type.name());
        request.addHeader("X-Active-Context", context.name());
        return request;
    }

    private AuthSession session(AccessContext context, ClientType type) {
        var user = new User();
        user.setId(3L);
        var session = new AuthSession();
        session.setId(77L);
        session.setSessionId("logout-test-session");
        session.setUser(user);
        session.setActiveContext(context);
        session.setClientType(type);
        session.setAudience(new AccessContextResolver().audience(context));
        session.setMfaVerified(context == AccessContext.STAFF);
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        return session;
    }
}
