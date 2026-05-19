package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record CreateMatchingRequestInput(
        String caseNumber,
        CreateMatchingRequestRequest request,
        Authentication authentication
) {
}
