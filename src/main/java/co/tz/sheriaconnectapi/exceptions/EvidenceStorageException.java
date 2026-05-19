package co.tz.sheriaconnectapi.exceptions;

public class EvidenceStorageException extends RuntimeException {
    public EvidenceStorageException() {
        super(ErrorMessages.EVIDENCE_STORAGE_FAILED.getMessage());
    }
}
