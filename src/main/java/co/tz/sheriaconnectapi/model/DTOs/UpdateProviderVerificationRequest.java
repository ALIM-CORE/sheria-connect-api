package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProviderVerificationRequest {
    private ProviderVerificationStatus verificationStatus;
    private ProviderAvailabilityStatus availabilityStatus;
    private Boolean active;
    private String notes;
}
