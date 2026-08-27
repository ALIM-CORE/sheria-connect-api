package co.tz.sheriaconnectapi.exceptions;

public class UnsupportedEvidenceTypeException extends DomainException {
    public UnsupportedEvidenceTypeException() {
        super(ErrorMessages.UNSUPPORTED_EVIDENCE_TYPE);
    }
}
