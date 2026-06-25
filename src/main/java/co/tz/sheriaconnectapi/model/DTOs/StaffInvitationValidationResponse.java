package co.tz.sheriaconnectapi.model.DTOs;

import java.time.Instant;
import java.util.List;

public record StaffInvitationValidationResponse(
        String name,
        String email,
        Instant expiresAt,
        List<String> roleDisplayNames,
        boolean existingAccount
) {
}
