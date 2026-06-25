package co.tz.sheriaconnectapi.model.DTOs;

public record MfaSetupResponse(
        String secret,
        String provisioningUri
) {
}
