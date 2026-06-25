package co.tz.sheriaconnectapi.model.DTOs;

import java.util.Set;
import java.time.Instant;

public record CreateStaffInvitationRequest(
        String name,
        String email,
        Set<Long> roleIds,
        String jobTitle,
        String department,
        String employeeNumber,
        String workEmail,
        Instant expiresAt,
        String reason
) {
}
