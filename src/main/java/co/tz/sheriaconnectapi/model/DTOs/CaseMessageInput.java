package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record CaseMessageInput(
        String caseNumber,
        Long matchingRequestId,
        CaseMessageRequest request,
        Authentication authentication
) {
}
