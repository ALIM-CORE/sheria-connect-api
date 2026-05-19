package co.tz.sheriaconnectapi.model.DTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMatchingRequestRequest {
    private Long providerProfileId;
    private String notes;
}
