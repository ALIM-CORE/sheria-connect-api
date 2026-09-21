package co.tz.sheriaconnectapi.exceptions;

public class DuplicateIncidentCategoryException extends DomainException {
    public DuplicateIncidentCategoryException() {
        super(ErrorMessages.DUPLICATE_INCIDENT_CATEGORY);
    }
}
