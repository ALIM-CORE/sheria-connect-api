package co.tz.sheriaconnectapi.model.Commands;

import lombok.Getter;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;

@Getter
public class MobileLoginResponse extends LoginResponse {

    private final String refreshToken;

    public MobileLoginResponse(
            String access,
            UserDTO user,
            String refreshToken
    ) {
        super(access, user);
        this.refreshToken = refreshToken;
    }
}
