package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record UpsertMyProviderProfileInput(
        CreateProviderProfileRequest request,
        Authentication authentication
) {
}
