package co.tz.sheriaconnectapi.exceptions;

public class EvidenceFileTooLargeException extends RuntimeException {
    public EvidenceFileTooLargeException() {
        super(ErrorMessages.EVIDENCE_FILE_TOO_LARGE.getMessage());
    }
}
