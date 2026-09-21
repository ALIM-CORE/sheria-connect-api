package co.tz.sheriaconnectapi.exceptions;

public class InvalidIncidentCategoryException extends DomainException {
    public InvalidIncidentCategoryException() {
        super(ErrorMessages.INVALID_INCIDENT_CATEGORY);
    }

    public InvalidIncidentCategoryException(String message) {
        super(ErrorMessages.INVALID_INCIDENT_CATEGORY, message);
    }
}
