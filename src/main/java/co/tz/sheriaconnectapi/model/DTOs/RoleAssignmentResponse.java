package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;

import java.time.Instant;

public record RoleAssignmentResponse(
        Long id,
        RoleSummaryResponse role,
        AccessContext context,
        RoleAssignmentStatus status,
        String grantedBy,
        String changedBy,
        String reason,
        Instant grantedAt,
        Instant activatedAt,
        Instant suspendedAt,
        Instant revokedAt,
        Instant expiresAt
) {
    public RoleAssignmentResponse(UserRoleAssignment assignment) {
        this(
                assignment.getId(),
                new RoleSummaryResponse(assignment.getRole(), 0),
                assignment.getContext(),
                assignment.getStatus(),
                assignment.getGrantedByUser() == null ? null : assignment.getGrantedByUser().getName(),
                assignment.getChangedByUser() == null ? null : assignment.getChangedByUser().getName(),
                assignment.getReason(),
                assignment.getGrantedAt(),
                assignment.getActivatedAt(),
                assignment.getSuspendedAt(),
                assignment.getRevokedAt(),
                assignment.getExpiresAt()
        );
    }
}
