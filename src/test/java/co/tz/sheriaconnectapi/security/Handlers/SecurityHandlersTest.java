package co.tz.sheriaconnectapi.security.Handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityHandlersTest {

    private ApiAuthenticationEntryPoint authenticationEntryPoint;
    private ApiAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        SecurityErrorResponseWriter writer =
                new SecurityErrorResponseWriter(jsonMapper);
        authenticationEntryPoint = new ApiAuthenticationEntryPoint(writer);
        accessDeniedHandler = new ApiAccessDeniedHandler(writer);
    }

    @Test
    void unauthenticatedRequestsReturnJson401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("expired")
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Authentication is required"));
    }

    @Test
    void authenticatedAuthorizationFailuresReturnJson403() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("forbidden")
        );

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Access denied"));
    }
}
