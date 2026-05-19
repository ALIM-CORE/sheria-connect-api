package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.model.Enums.IncidentType;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;

import java.time.Instant;

public record AdminIncidentReportSearchInput(
        IncidentReportStatus status,
        IncidentUrgency urgency,
        IncidentType incidentType,
        AnonymityMode anonymityMode,
        Boolean matchingRequested,
        Instant createdFrom,
        Instant createdTo
) {
}
