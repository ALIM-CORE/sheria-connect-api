package co.tz.sheriaconnectapi.exceptions;

public class IncidentCategoryNotFoundException extends DomainException {
    public IncidentCategoryNotFoundException() {
        super(ErrorMessages.INCIDENT_CATEGORY_NOT_FOUND);
    }
}
