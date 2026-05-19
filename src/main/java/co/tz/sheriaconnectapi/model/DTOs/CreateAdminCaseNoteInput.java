package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record CreateAdminCaseNoteInput(
        String caseNumber,
        CreateAdminCaseNoteRequest request,
        Authentication authentication
) {
}
