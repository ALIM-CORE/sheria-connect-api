package co.tz.sheriaconnectapi.exceptions;

public class EvidenceStorageException extends DomainException {
    public EvidenceStorageException() {
        super(ErrorMessages.EVIDENCE_STORAGE_FAILED);
    }
}
