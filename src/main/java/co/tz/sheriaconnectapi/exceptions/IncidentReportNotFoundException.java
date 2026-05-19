package co.tz.sheriaconnectapi.exceptions;

public class IncidentReportNotFoundException extends RuntimeException {
    public IncidentReportNotFoundException() {
        super(ErrorMessages.INCIDENT_REPORT_NOT_FOUND.getMessage());
    }
}
