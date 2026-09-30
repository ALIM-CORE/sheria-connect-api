package co.tz.sheriaconnectapi.services.AuthServices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.security.Jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebContextSessionsIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private UserRoleAssignmentRepository assignments;
    @Autowired private PasswordEncoder passwords;
    @Autowired private TotpService totp;
    private final ObjectMapper json = new ObjectMapper();
    private User user;

    @BeforeEach
    void sharedIdentity() {
        user = new User();
        user.setName("Web context test");
        user.setEmail("web-context-" + UUID.randomUUID() + "@example.test");
        user.setPassword(passwords.encode("Integration-only-password-123!"));
        user.setEmailVerified(true);
        user = users.save(user);
        for (AccessContext context : new AccessContext[]{AccessContext.CITIZEN, AccessContext.STAFF}) {
            var role = roles.findByName(context == AccessContext.STAFF ? "SUPER_ADMIN" : "CITIZEN").orElseThrow();
            var assignment = new UserRoleAssignment();
            assignment.setUser(user);
            assignment.setRole(role);
            assignment.setContext(context);
            assignment.setStatus(RoleAssignmentStatus.ACTIVE);
            assignment.setActivatedAt(Instant.now());
            assignments.save(assignment);
        }
    }

    @Test
    void citizenAndMfaStaffSessionsRefreshIndependentlyAndLogoutDoesNotCrossContexts() throws Exception {
        var citizenLogin = login("WEB", "CITIZEN");
        assertEquals("AUTHENTICATED", body(citizenLogin).get("state").asText());
        assertFalse(body(citizenLogin).has("refreshToken"));
        assertEquals("CITIZEN", body(citizenLogin).get("user").get("activeContext").asText());
        var citizenCookie = cookie(citizenLogin, "refresh_token_citizen");

        var passwordStep = login("WEB", "STAFF");
        assertEquals("MFA_SETUP_REQUIRED", body(passwordStep).get("state").asText());
        assertTrue(passwordStep.getResponse().getHeaders("Set-Cookie").isEmpty());
        assertTrue(body(passwordStep).get("access").isNull());
        String challenge = body(passwordStep).get("challengeToken").asText();
        var setup = request("/auth/mfa/setup", "WEB", "STAFF", Map.of("challengeToken", challenge));
        String secret = body(setup).get("secret").asText();
        String code = ReflectionTestUtils.invokeMethod(totp, "generate", secret, Instant.now().getEpochSecond() / 30);
        var confirmed = request("/auth/mfa/setup/confirm", "WEB", "STAFF",
                Map.of("challengeToken", challenge, "code", code));
        assertEquals(200, confirmed.getResponse().getStatus(), confirmed.getResponse().getContentAsString());
        assertEquals("AUTHENTICATED", body(confirmed).get("state").asText());
        assertEquals(true, JwtUtil.getClaims(body(confirmed).get("access").asText()).get("mfa_verified", Boolean.class));
        var staffCookie = cookie(confirmed, "refresh_token_staff");

        var staffRefresh = refresh("STAFF", staffCookie, citizenCookie);
        assertEquals(200, staffRefresh.getResponse().getStatus(), staffRefresh.getResponse().getContentAsString());
        assertEquals("STAFF", body(staffRefresh).get("user").get("activeContext").asText());
        assertEquals(1, staffRefresh.getResponse().getHeaders("Set-Cookie").size());
        staffCookie = cookie(staffRefresh, "refresh_token_staff");
        var citizenRefresh = refresh("CITIZEN", staffCookie, citizenCookie);
        assertEquals(200, citizenRefresh.getResponse().getStatus(), citizenRefresh.getResponse().getContentAsString());
        assertEquals("CITIZEN", body(citizenRefresh).get("user").get("activeContext").asText());
        assertEquals(1, citizenRefresh.getResponse().getHeaders("Set-Cookie").size());
        citizenCookie = cookie(citizenRefresh, "refresh_token_citizen");

        var logout = mvc.perform(post("/auth/logout")
                .header("X-Client-Type", "WEB").header("X-Active-Context", "CITIZEN")
                .header("Authorization", "Bearer expired-or-invalid-token")
                .cookie(staffCookie, citizenCookie)).andReturn();
        assertEquals(200, logout.getResponse().getStatus());
        assertEquals(1, logout.getResponse().getHeaders("Set-Cookie").size());
        assertTrue(logout.getResponse().getHeader("Set-Cookie").startsWith("refresh_token_citizen=;"));
        assertEquals(200, refresh("STAFF", staffCookie).getResponse().getStatus());
        assertNotEquals(200, refresh("CITIZEN", citizenCookie).getResponse().getStatus());
    }

    @Test
    void staffCookieCannotBeRelabelledToObtainACitizenToken() throws Exception {
        // Exercise wrong-context rejection with a real DB session/token.
        var citizenLogin = login("WEB", "CITIZEN");
        var citizenCookie = cookie(citizenLogin, "refresh_token_citizen");
        var relabelled = new Cookie("refresh_token_staff", citizenCookie.getValue());
        var rejected = refresh("STAFF", relabelled);
        assertEquals(400, rejected.getResponse().getStatus());
        assertEquals("INVALID_TOKEN", json.readTree(rejected.getResponse().getContentAsString()).get("errorCode").asText());
        assertEquals(200, refresh("CITIZEN", citizenCookie).getResponse().getStatus());
    }

    @Test
    void mobileLoginAndRefreshStillUseBodyTokensWithoutCookies() throws Exception {
        var mobileLogin = login("MOBILE", "PROVIDER");
        assertEquals("PROVIDER", body(mobileLogin).get("user").get("activeContext").asText());
        assertTrue(mobileLogin.getResponse().getHeaders("Set-Cookie").isEmpty());
        String refreshToken = body(mobileLogin).get("refreshToken").asText();
        var refreshed = request("/auth/refresh", "MOBILE", "PROVIDER", Map.of("refreshToken", refreshToken));
        assertEquals(200, refreshed.getResponse().getStatus(), refreshed.getResponse().getContentAsString());
        assertTrue(refreshed.getResponse().getHeaders("Set-Cookie").isEmpty());
        assertTrue(body(refreshed).get("refresh").asText().length() > 0);
        assertEquals("PROVIDER", body(refreshed).get("user").get("activeContext").asText());
    }

    private MvcResult login(String client, String context) throws Exception {
        var result = request("/auth/login", client, context,
                Map.of("email", user.getEmail(), "password", "Integration-only-password-123!"));
        assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return result;
    }

    private MvcResult refresh(String context, Cookie... cookies) throws Exception {
        return mvc.perform(post("/auth/refresh").header("X-Client-Type", "WEB")
                .header("X-Active-Context", context).cookie(cookies)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();
    }

    private MvcResult request(String path, String client, String context, Map<String, String> payload) throws Exception {
        return mvc.perform(post(path).header("X-Client-Type", client).header("X-Active-Context", context)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get("body");
    }

    private Cookie cookie(MvcResult result, String name) {
        String header = result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith(name + "=")).findFirst().orElseThrow();
        return new Cookie(name, header.substring(name.length() + 1, header.indexOf(';')));
    }
}
