package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.AuthSession;

public record IssuedSession(
        String accessToken,
        String refreshToken,
        UserDTO user,
        AuthSession session
) {
}
