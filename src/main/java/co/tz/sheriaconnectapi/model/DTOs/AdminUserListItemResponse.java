package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.UserAccountType;

import java.time.Instant;
import java.util.List;

public record AdminUserListItemResponse(
        Long id,
        String name,
        String email,
        UserAccountType accountType,
        boolean active,
        boolean locked,
        boolean emailVerified,
        String suspensionReason,
        Instant suspendedAt,
        Instant lastLoginAt,
        Instant createdAt,
        List<RoleSummaryResponse> roles,
        long reportCount,
        long storyCount,
        ProviderAccountSummary provider,
        CapabilitySummary capabilities,
        StaffAccessSummary staffAccess,
        List<LinkedProductAccountSummary> linkedProductAccounts
) {
    public static AdminUserListItemResponse basic(
            User user,
            List<RoleSummaryResponse> roles,
            long reportCount,
            long storyCount,
            ProviderAccountSummary provider,
            CapabilitySummary capabilities,
            StaffAccessSummary staffAccess,
            List<LinkedProductAccountSummary> linkedProductAccounts
    ) {
        return new AdminUserListItemResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAccountType(),
                Boolean.TRUE.equals(user.getActive()),
                Boolean.TRUE.equals(user.getLocked()),
                Boolean.TRUE.equals(user.getEmailVerified()),
                user.getSuspensionReason(),
                user.getSuspendedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                roles,
                reportCount,
                storyCount,
                provider,
                capabilities,
                staffAccess,
                linkedProductAccounts
        );
    }

    public record ProviderAccountSummary(
            Long profileId,
            String providerType,
            String verificationStatus,
            String availabilityStatus,
            String displayName
    ) {
    }

    public record StaffAccessSummary(
            String state,
            Long invitationId,
            Long staffUserId,
            String staffName,
            String staffEmail,
            String jobTitle,
            String department,
            Instant expiresAt,
            boolean mfaConfigured
    ) {
    }

    public record CapabilitySummary(
            boolean citizen,
            boolean provider,
            boolean staff
    ) {
    }

    public record LinkedProductAccountSummary(
            Long userId,
            String name,
            String email,
            UserAccountType accountType
    ) {
    }
}
