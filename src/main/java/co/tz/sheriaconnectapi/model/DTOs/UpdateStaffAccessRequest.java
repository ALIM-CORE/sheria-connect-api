package co.tz.sheriaconnectapi.model.DTOs;

public record UpdateStaffAccessRequest(
        String action,
        String reason
) {
}
