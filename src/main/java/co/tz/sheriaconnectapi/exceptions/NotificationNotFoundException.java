package co.tz.sheriaconnectapi.exceptions;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() {
        super(ErrorMessages.NOTIFICATION_NOT_FOUND.getMessage());
    }
}
