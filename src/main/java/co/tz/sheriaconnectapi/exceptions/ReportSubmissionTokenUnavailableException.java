package co.tz.sheriaconnectapi.exceptions;

public class ReportSubmissionTokenUnavailableException extends DomainException {
    public ReportSubmissionTokenUnavailableException() {
        super(ErrorMessages.REPORT_SUBMISSION_TOKEN_UNAVAILABLE);
    }
}
