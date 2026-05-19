package co.tz.sheriaconnectapi.exceptions;

import lombok.Getter;

@Getter
public enum ErrorMessages {
    MEMBER_NOT_FOUND("Member not found"),
    ROLE_NOT_FOUND("Role not found"),
    AUTHORITY_NOT_FOUND("Authority not found"),
    EMAIL_ALREADY_EXISTS("Email already exists"),
    INVALID_CLIENT_TYPE("Invalid client type"),
    INVALID_TOKEN("Invalid token"),
    MISSING_TOKEN("Missing refresh token"),
    EMAIL_ALREADY_VERIFIED("Email already verified"),
    EMAIL_NOT_VERIFIED("This email has not been verified yet"),
    EMAIL_VERIFICATION_TOKEN_EXPIRED("Email verification token expired"),
    EMAIL_VERIFICATION_TOKEN_INVALID("Invalid email verification token"),
    PASSWORD_RESET_TOKEN_INVALID("Invalid password reset token"),
    PASSWORD_RESET_TOKEN_EXPIRED("Password reset token expired"),
    INVALID_LOGIN_CREDENTIALS("Wrong email or password"),
    INCIDENT_REPORT_NOT_FOUND("Incident report not found"),
    INVALID_CASE_NUMBER("Invalid case number"),
    INVALID_TRACKING_TOKEN("Invalid tracking token"),
    UNAUTHORIZED_CASE_ACCESS("You are not allowed to access this case"),
    MISSING_EVIDENCE_FILE("Evidence file is required"),
    UNSUPPORTED_EVIDENCE_TYPE("Unsupported evidence file type"),
    EVIDENCE_FILE_TOO_LARGE("Evidence file is too large"),
    EVIDENCE_STORAGE_FAILED("Evidence file could not be stored"),
    INVALID_STATUS_TRANSITION("Invalid case status transition"),
    PROVIDER_PROFILE_NOT_FOUND("Provider profile not found"),
    MATCHING_REQUEST_NOT_FOUND("Matching request not found"),
    INVALID_MATCHING_REQUEST_STATUS("Invalid matching request status"),
    DUPLICATE_MATCHING_REQUEST("This provider has already been matched to the case"),
    ;

    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }

}
