package co.tz.sheriaconnectapi.exceptions;

public class EmailDeliveryException extends DomainException {

    public EmailDeliveryException(Throwable cause) {
        super(ErrorMessages.EMAIL_DELIVERY_FAILED, cause);
    }
}
