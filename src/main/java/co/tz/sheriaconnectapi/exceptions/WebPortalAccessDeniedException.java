package co.tz.sheriaconnectapi.exceptions;

public class WebPortalAccessDeniedException extends RuntimeException {
    public WebPortalAccessDeniedException() {
        super(ErrorMessages.WEB_PORTAL_ACCESS_DENIED.getMessage());
    }
}
