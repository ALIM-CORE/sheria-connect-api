package co.tz.sheriaconnectapi.model.DTOs;

public record UpdateProviderVerificationInput(
        Long providerProfileId,
        UpdateProviderVerificationRequest request
) {
}
