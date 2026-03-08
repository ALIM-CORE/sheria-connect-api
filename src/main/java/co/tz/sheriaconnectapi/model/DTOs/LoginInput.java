package co.tz.sheriaconnectapi.model.DTOs;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginInput {
    private UserLoginDTO  userLoginDTO;
    private HttpServletRequest request;
}
