package co.tz.sheriaconnectapi.services.AuthServices;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpServiceTest {
    private static final String RFC_6238_SECRET =
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private final TotpService service = new TotpService("Sheria Connect");

    @Test
    void verifiesStandardTotpAndReturnsItsTimeStep() {
        var counter = service.verifiedCounter(
                RFC_6238_SECRET,
                "287082",
                Instant.ofEpochSecond(59)
        );

        assertTrue(counter.isPresent());
        assertEquals(1L, counter.getAsLong());
    }

    @Test
    void acceptsOneTimeStepOfClockDrift() {
        var counter = service.verifiedCounter(
                RFC_6238_SECRET,
                "287082",
                Instant.ofEpochSecond(89)
        );

        assertTrue(counter.isPresent());
        assertEquals(1L, counter.getAsLong());
    }

    @Test
    void rejectsMalformedCodes() {
        assertTrue(service.verifiedCounter(
                RFC_6238_SECRET,
                "not-a-code",
                Instant.ofEpochSecond(59)
        ).isEmpty());
    }
}
