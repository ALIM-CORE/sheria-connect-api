package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.LegalServiceProviderType;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;

public record ProviderProfileSearchInput(
        LegalServiceProviderType providerType,
        ProviderVerificationStatus verificationStatus,
        ProviderAvailabilityStatus availabilityStatus,
        Boolean active
) {
}
