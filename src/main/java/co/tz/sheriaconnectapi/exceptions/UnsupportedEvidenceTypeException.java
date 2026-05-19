package co.tz.sheriaconnectapi.exceptions;

public class UnsupportedEvidenceTypeException extends RuntimeException {
    public UnsupportedEvidenceTypeException() {
        super(ErrorMessages.UNSUPPORTED_EVIDENCE_TYPE.getMessage());
    }
}
