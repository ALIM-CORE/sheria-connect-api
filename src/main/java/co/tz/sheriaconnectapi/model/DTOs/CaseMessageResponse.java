package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.CaseMessage;
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
                message.getSenderUser() == null ? null : message.getSenderUser().getId(),
                message.getSenderUser() == null ? null : message.getSenderUser().getName(),
                message.getSenderRole(),
                message.getBody(),
                message.getCreatedAt()
        );
    }
}
