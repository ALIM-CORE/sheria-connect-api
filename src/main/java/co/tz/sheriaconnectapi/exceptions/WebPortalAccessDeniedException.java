package co.tz.sheriaconnectapi.exceptions;

public class WebPortalAccessDeniedException extends DomainException {
    public WebPortalAccessDeniedException() {
        super(ErrorMessages.WEB_PORTAL_ACCESS_DENIED);
    }
}
