package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.IncidentReportReply;

import java.time.Instant;

public record IncidentReportReplyResponse(
        Long id,
        Long needsInfoHistoryId,
        String body,
        Instant createdAt
) {
    public IncidentReportReplyResponse(IncidentReportReply reply) {
        this(
                reply.getId(),
                reply.getNeedsInfoHistory().getId(),
                reply.getBody(),
                reply.getCreatedAt()
        );
    }
}
