package co.tz.sheriaconnectapi.security.Access;

import co.tz.sheriaconnectapi.exceptions.InvalidClientTypeException;
import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class AccessContextResolverTest {
    private final AccessContextResolver resolver = new AccessContextResolver();

    @Test
    void explicitCitizenWebIsSupportedAndLegacyWebStillDefaultsToStaff() {
        var request = new MockHttpServletRequest();
        assertEquals(AccessContext.STAFF, resolver.context(request, ClientType.WEB, null));
        request.addHeader("X-Active-Context", "CITIZEN");
        assertEquals(AccessContext.CITIZEN, resolver.context(request, ClientType.WEB, null));
    }

    @Test
    void providerWebAndStaffMobileAreStillRejected() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Active-Context", "PROVIDER");
        assertThrows(InvalidClientTypeException.class, () -> resolver.context(request, ClientType.WEB, null));
        request.removeHeader("X-Active-Context");
        request.addHeader("X-Active-Context", "STAFF");
        assertThrows(InvalidClientTypeException.class, () -> resolver.context(request, ClientType.MOBILE, null));
    }

    @Test
    void sessionMustMatchBothClientTypeAndContext() {
        var session = new AuthSession();
        session.setClientType(ClientType.WEB);
        session.setActiveContext(AccessContext.STAFF);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Client-Type", "WEB");
        request.addHeader("X-Active-Context", "CITIZEN");
        assertFalse(resolver.matchesSession(request, session));
        request.removeHeader("X-Client-Type");
        request.removeHeader("X-Active-Context");
        request.addHeader("X-Client-Type", "MOBILE");
        request.addHeader("X-Active-Context", "PROVIDER");
        assertFalse(resolver.matchesSession(request, session));
        request.removeHeader("X-Client-Type");
        request.removeHeader("X-Active-Context");
        request.addHeader("X-Client-Type", "WEB");
        request.addHeader("X-Active-Context", "STAFF");
        assertTrue(resolver.matchesSession(request, session));
    }

    @Test
    void mobileRefreshWithoutContextUsesTheExistingProductSession() {
        var session = new AuthSession();
        session.setClientType(ClientType.MOBILE);
        session.setActiveContext(AccessContext.PROVIDER);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Client-Type", "MOBILE");
        assertTrue(resolver.matchesSession(request, session));
    }
}
