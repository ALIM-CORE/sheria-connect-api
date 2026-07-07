package co.tz.sheriaconnectapi.exceptions;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(Throwable cause) {
        super(ErrorMessages.EMAIL_DELIVERY_FAILED.getMessage(), cause);
    }
}
