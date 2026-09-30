package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenCookieServiceTest {
    private final RefreshTokenCookieService cookies = new RefreshTokenCookieService(true, "Lax", "/");

    @Test
    void contextsHaveDistinctHostOnlySecureHttpOnlyCookies() {
        assertEquals(3, Arrays.stream(AccessContext.values()).map(cookies::name).distinct().count());
        for (AccessContext context : AccessContext.values()) {
            String header = cookies.create(context, "token", Duration.ofDays(7));
            assertTrue(header.startsWith(cookies.name(context) + "=token;"));
            assertTrue(header.contains("HttpOnly"));
            assertTrue(header.contains("Secure"));
            assertTrue(header.contains("SameSite=Lax"));
            assertTrue(header.contains("Path=/"));
            assertFalse(header.contains("Domain="));
        }
    }

    @Test
    void selectionNeverUsesAnotherContextCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token_staff", "staff"),
                new Cookie("refresh_token_citizen", "citizen"), new Cookie("refresh_token", "legacy"));
        assertEquals("staff", cookies.read(request, AccessContext.STAFF));
        assertEquals("citizen", cookies.read(request, AccessContext.CITIZEN));
        assertNull(cookies.read(request, AccessContext.PROVIDER));
        assertEquals("legacy", cookies.readLegacy(request));
    }

    @Test
    void clearingOneContextDoesNotClearOtherCookies() {
        assertTrue(cookies.clear(AccessContext.CITIZEN).startsWith("refresh_token_citizen=;"));
        assertTrue(cookies.clear(AccessContext.STAFF).contains("Max-Age=0"));
        assertTrue(cookies.clearLegacy().startsWith("refresh_token=;"));
    }
}
