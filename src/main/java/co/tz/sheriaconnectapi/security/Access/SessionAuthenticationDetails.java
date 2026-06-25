package co.tz.sheriaconnectapi.security.Access;

import co.tz.sheriaconnectapi.model.Enums.AccessContext;

public record SessionAuthenticationDetails(
        String sessionId,
        AccessContext context,
        String audience,
        boolean mfaVerified
) {
}
