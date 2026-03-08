package co.tz.sheriaconnectapi.model.DTOs;

import lombok.Getter;

@Getter
public class ResetPasswordDTO {
    private String token;
    private String newPassword;
}
