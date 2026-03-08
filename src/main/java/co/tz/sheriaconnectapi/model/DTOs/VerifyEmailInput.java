package co.tz.sheriaconnectapi.model.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VerifyEmailInput {
    private String token;
}
