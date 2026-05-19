package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateIncidentReportStatusRequest {
    private IncidentReportStatus status;
    private String note;
}
