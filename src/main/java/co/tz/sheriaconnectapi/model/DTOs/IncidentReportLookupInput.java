package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record IncidentReportLookupInput(
        String caseNumber,
        String trackingToken,
        Authentication authentication,
        boolean adminAccess
) {
}
