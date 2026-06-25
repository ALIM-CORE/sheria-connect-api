package co.tz.sheriaconnectapi.model.DTOs;

public record UpdateProviderVerificationInput(
        Long providerProfileId,
        UpdateProviderVerificationRequest request,
        org.springframework.security.core.Authentication authentication
) {
}
