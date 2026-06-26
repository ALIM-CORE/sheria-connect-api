package co.tz.sheriaconnectapi.exceptions;

public class NotificationAccessDeniedException extends RuntimeException {
    public NotificationAccessDeniedException() {
        super("You are not allowed to access this notification");
    }
}
