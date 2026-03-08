package co.tz.sheriaconnectapi.model.Commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String access;
    private UserDTO user;
}
