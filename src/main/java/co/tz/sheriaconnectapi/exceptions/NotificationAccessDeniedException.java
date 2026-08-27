package co.tz.sheriaconnectapi.exceptions;

public class NotificationAccessDeniedException extends DomainException {
    public NotificationAccessDeniedException() {
        super(ErrorMessages.NOTIFICATION_ACCESS_DENIED);
    }
}
