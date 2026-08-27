package co.tz.sheriaconnectapi.exceptions;

public class EvidenceFileNotFoundException extends DomainException {
    public EvidenceFileNotFoundException() {
        super(ErrorMessages.EVIDENCE_FILE_NOT_FOUND);
    }
}
