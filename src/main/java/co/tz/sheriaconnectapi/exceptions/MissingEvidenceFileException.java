package co.tz.sheriaconnectapi.exceptions;

public class MissingEvidenceFileException extends DomainException {
    public MissingEvidenceFileException() {
        super(ErrorMessages.MISSING_EVIDENCE_FILE);
    }
}
