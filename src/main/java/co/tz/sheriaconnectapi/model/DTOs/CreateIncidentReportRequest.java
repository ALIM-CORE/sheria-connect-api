package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.IncidentType;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateIncidentReportRequest {
    private AnonymityMode anonymityMode;
    private IncidentType incidentType;
    private IncidentUrgency urgency;
    private String title;
    private String description;
    private LocalDate incidentDate;
    private String locationDescription;
    private String region;
    private String district;
    private String ward;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String pseudonym;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private Boolean matchingRequested;
}
