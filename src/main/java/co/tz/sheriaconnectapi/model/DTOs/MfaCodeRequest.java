package co.tz.sheriaconnectapi.model.DTOs;

public record MfaCodeRequest(
        String challengeToken,
        String code,
        String recoveryCode
) {
}
