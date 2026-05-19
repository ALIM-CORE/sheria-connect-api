package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record UpdateMatchingRequestStatusInput(
        Long matchingRequestId,
        UpdateMatchingRequestStatusRequest request,
        Authentication authentication
) {
}
