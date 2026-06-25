package co.tz.sheriaconnectapi.model.DTOs;

public record AcceptStaffInvitationRequest(
        String token,
        String password
) {
}
