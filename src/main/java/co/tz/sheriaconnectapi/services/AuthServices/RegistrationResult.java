package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.Entities.User;

public record RegistrationResult(
        User user,
        boolean verificationEmailSent
) {
}
