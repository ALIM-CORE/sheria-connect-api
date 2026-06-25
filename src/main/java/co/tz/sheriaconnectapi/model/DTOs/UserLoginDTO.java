package co.tz.sheriaconnectapi.model.DTOs;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserLoginDTO {
    private String email;
    private String password;
    private String invitationToken;

    public UserLoginDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

}
