package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import lombok.Getter;

import java.time.Instant;

@Getter
public class CaseStatusHistoryResponse {
    private final Long id;
    private final IncidentReportStatus fromStatus;
    private final IncidentReportStatus toStatus;
    private final String changedByEmail;
    private final String note;
    private final Instant createdAt;

    public CaseStatusHistoryResponse(CaseStatusHistory history) {
        this.id = history.getId();
        this.fromStatus = history.getFromStatus();
        this.toStatus = history.getToStatus();
        this.changedByEmail = history.getChangedByUser() == null
                ? null
                : history.getChangedByUser().getEmail();
        this.note = history.getNote();
        this.createdAt = history.getCreatedAt();
    }
}
