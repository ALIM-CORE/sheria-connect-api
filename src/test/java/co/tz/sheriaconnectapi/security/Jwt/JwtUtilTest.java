package co.tz.sheriaconnectapi.security.Jwt;

import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {
    static {
        System.setProperty(
                "JWT_SECRET",
                "test-only-jwt-secret-that-is-long-enough-for-hs512-signing-1234567890"
        );
    }

    @Test
    void sessionTokenUsesAStandardsCompliantAudienceCollection() {
        User user = new User();
        user.setId(41L);

        AuthSession session = new AuthSession();
        session.setActiveContext(AccessContext.STAFF);
        session.setClientType(ClientType.WEB);
        session.setAudience("sheria-connect-portal");
        session.setMfaVerified(true);
        session.setExpiresAt(Instant.now().plusSeconds(3600));

        String token = JwtUtil.generateAccessToken(user, session);
        var claims = JwtUtil.getClaims(token);

        assertEquals("41", claims.getSubject());
        assertTrue(claims.getAudience().contains("sheria-connect-portal"));
        assertEquals(session.getSessionId(), claims.get("sid", String.class));
    }
}
