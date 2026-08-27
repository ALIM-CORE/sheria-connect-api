package co.tz.sheriaconnectapi.exceptions;

public class NotificationNotFoundException extends DomainException {
    public NotificationNotFoundException() {
        super(ErrorMessages.NOTIFICATION_NOT_FOUND);
    }
}
