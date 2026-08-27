package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record CaseMessageInput(
        String caseNumber,
        Long matchingRequestId,
        Long afterId,
        CaseMessageRequest request,
        Authentication authentication
) {
}
