package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.LegalServiceProviderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderRegisterRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
    private LegalServiceProviderType providerType;
}
