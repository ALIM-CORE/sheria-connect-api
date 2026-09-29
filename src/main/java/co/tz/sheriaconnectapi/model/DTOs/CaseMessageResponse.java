package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.CaseMessage;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.CaseMessageSenderRole;

import java.time.Instant;

public record CaseMessageResponse(
        Long id,
        String caseNumber,
        Long matchingRequestId,
        Long senderUserId,
        String senderName,
        CaseMessageSenderRole senderRole,
        String body,
        Instant createdAt
) {
    public CaseMessageResponse(CaseMessage message) {
        this(
                message.getId(),
                message.getIncidentReport().getCaseNumber(),
                message.getCaseMatchRequest() == null ? null : message.getCaseMatchRequest().getId(),
                senderUserId(message),
                senderName(message),
                message.getSenderRole(),
                message.getBody(),
                message.getCreatedAt()
        );
    }

    private static Long senderUserId(CaseMessage message) {
        if (message.getSenderRole() == CaseMessageSenderRole.CITIZEN) {
            return null;
        }
        return message.getSenderUser() == null ? null : message.getSenderUser().getId();
    }

    private static String senderName(CaseMessage message) {
        if (message.getSenderRole() != CaseMessageSenderRole.CITIZEN) {
            return message.getSenderUser() == null ? null : message.getSenderUser().getName();
        }

        IncidentReport report = message.getIncidentReport();
        if (report == null || report.getAnonymityMode() == null) {
            return null;
        }

        return switch (report.getAnonymityMode()) {
            case NAMED -> nonBlankOrNull(report.getContactName());
            case PSEUDONYMOUS -> nonBlankOrNull(report.getPseudonym());
            case FULLY_ANONYMOUS -> null;
        };
    }

    private static String nonBlankOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
