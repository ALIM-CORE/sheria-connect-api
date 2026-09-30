package co.tz.sheriaconnectapi.exceptions;

public class ReportSubmissionIdReusedException extends DomainException {
    public ReportSubmissionIdReusedException() {
        super(ErrorMessages.REPORT_SUBMISSION_ID_REUSED);
    }
}
