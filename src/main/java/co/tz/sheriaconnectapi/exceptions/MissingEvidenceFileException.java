package co.tz.sheriaconnectapi.exceptions;

public class MissingEvidenceFileException extends RuntimeException {
    public MissingEvidenceFileException() {
        super(ErrorMessages.MISSING_EVIDENCE_FILE.getMessage());
    }
}
