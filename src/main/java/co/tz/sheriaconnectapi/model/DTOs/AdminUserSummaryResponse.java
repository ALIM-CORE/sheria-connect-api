package co.tz.sheriaconnectapi.model.DTOs;

public record AdminUserSummaryResponse(
        long totalStaff,
        long activeStaff,
        long totalCitizens,
        long activeCitizens,
        long totalProviders,
        long activeProviders,
        long lockedAccounts,
        long pendingInvitations
) {
}
