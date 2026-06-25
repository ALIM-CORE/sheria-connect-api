package co.tz.sheriaconnectapi.model.Commands;

import lombok.Getter;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Enums.AuthState;

import java.util.List;

@Getter
public class LoginResponse {
    private String access;
    private UserDTO user;
    private AuthState state;
    private String challengeToken;
    private List<String> recoveryCodes;

    public LoginResponse(String access, UserDTO user) {
        this(access, user, AuthState.AUTHENTICATED, null);
    }

    public LoginResponse(
            String access,
            UserDTO user,
            AuthState state,
            String challengeToken
    ) {
        this(access, user, state, challengeToken, List.of());
    }

    public LoginResponse(
            String access,
            UserDTO user,
            AuthState state,
            String challengeToken,
            List<String> recoveryCodes
    ) {
        this.access = access;
        this.user = user;
        this.state = state;
        this.challengeToken = challengeToken;
        this.recoveryCodes = recoveryCodes == null ? List.of() : recoveryCodes;
    }
}
