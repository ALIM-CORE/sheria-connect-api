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
    ;

    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }

}
