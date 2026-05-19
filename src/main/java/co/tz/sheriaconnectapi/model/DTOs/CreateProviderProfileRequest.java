package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.IncidentType;
import co.tz.sheriaconnectapi.model.Enums.LegalServiceProviderType;
import co.tz.sheriaconnectapi.model.Enums.PricingTier;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CreateProviderProfileRequest {
    private Long userId;
    private LegalServiceProviderType providerType;
    private String displayName;
    private String organizationName;
    private String email;
    private String phone;
    private ProviderVerificationStatus verificationStatus;
    private Set<IncidentType> specialties;
    private Set<String> regions;
    private ProviderAvailabilityStatus availabilityStatus;
    private PricingTier pricingTier;
    private Boolean active;
    private Integer currentWorkload;
    private Integer maxActiveCases;
    private String notes;
}
