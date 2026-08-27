package co.tz.sheriaconnectapi.exceptions;

public class EvidenceFileNotFoundException extends RuntimeException {
    public EvidenceFileNotFoundException() {
        super(ErrorMessages.EVIDENCE_FILE_NOT_FOUND.getMessage());
    }
}
