package co.tz.sheriaconnectapi.services.IncidentReportServices;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingTokenServiceTest {

    private final TrackingTokenService service = new TrackingTokenService("test-derivation-key");

    @Test
    void generatesTwentyCharacterCrockfordTokenInWritableGroups() {
        String token = service.generateToken();

        assertTrue(token.matches("[0-9A-HJKMNP-TV-Z]{4}(-[0-9A-HJKMNP-TV-Z]{4}){4}"));
        assertEquals(20, token.replace("-", "").length());
    }

    @Test
    void acceptsCaseSpacingHyphensAndHandwritingAliases() {
        String original = "AB10-CDEF-GHJK-MNPQ-RSTV";
        String expectedHash = service.hash(original);

        assertTrue(service.matches("abio cdef-ghjk mnpq-rstv", expectedHash));
    }

    @Test
    void stillAcceptsExistingRawLowercaseHexTokens() throws Exception {
        String legacy = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String legacyHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(legacy.getBytes(StandardCharsets.UTF_8))
        );

        assertTrue(service.matches(legacy, legacyHash));
    }

    @Test
    void derivesStableDistinctTokensFromSubmissionIds() {
        UUID first = UUID.fromString("a3b7f7eb-68fe-49ee-bb37-c858966c3d02");
        UUID second = UUID.fromString("950f40e6-67ca-4cd4-8fb2-da671146c72e");

        assertEquals(service.deriveToken(first), service.deriveToken(first));
        assertNotEquals(service.deriveToken(first), service.deriveToken(second));
    }
}
