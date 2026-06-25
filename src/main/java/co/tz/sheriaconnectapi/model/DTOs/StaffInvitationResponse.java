package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.StaffInvitation;
import co.tz.sheriaconnectapi.model.Enums.StaffInvitationStatus;

import java.time.Instant;
import java.util.List;

public record StaffInvitationResponse(
        Long id,
        String name,
        String email,
        StaffInvitationStatus status,
        Instant expiresAt,
        Instant createdAt,
        String invitedBy,
        Long linkedProductUserId,
        String linkedProductUserName,
        String linkedProductAccountType,
        String jobTitle,
        String department,
        String employeeNumber,
        String workEmail,
        Instant accessExpiresAt,
        String grantReason,
        List<RoleSummaryResponse> roles
) {
    public StaffInvitationResponse(StaffInvitation invitation) {
        this(
                invitation.getId(),
                invitation.getName(),
                invitation.getEmail(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt(),
                invitation.getInvitedByUser() == null ? null : invitation.getInvitedByUser().getName(),
                invitation.getLinkedProductUser() == null ? null : invitation.getLinkedProductUser().getId(),
                invitation.getLinkedProductUser() == null ? null : invitation.getLinkedProductUser().getName(),
                invitation.getLinkedProductUser() == null
                        ? null
                        : invitation.getLinkedProductUser().getAccountType().name(),
                invitation.getJobTitle(),
                invitation.getDepartment(),
                invitation.getEmployeeNumber(),
                invitation.getWorkEmail(),
                invitation.getAccessExpiresAt(),
                invitation.getGrantReason(),
                invitation.getRoles().stream()
                        .map(role -> new RoleSummaryResponse(role, 0))
                        .toList()
        );
    }
}
