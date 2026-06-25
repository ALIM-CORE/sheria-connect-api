package co.tz.sheriaconnectapi.model.DTOs;

public record UpdateUserStatusRequest(
        Boolean active,
        Boolean locked,
        String reason
) {
}
