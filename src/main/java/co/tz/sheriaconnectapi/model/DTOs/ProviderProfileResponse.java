package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Enums.IncidentType;
import co.tz.sheriaconnectapi.model.Enums.LegalServiceProviderType;
import co.tz.sheriaconnectapi.model.Enums.PricingTier;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

@Getter
public class ProviderProfileResponse {
    private final Long id;
    private final Long userId;
    private final String userEmail;
    private final LegalServiceProviderType providerType;
    private final String displayName;
    private final String organizationName;
    private final String email;
    private final String phone;
    private final ProviderVerificationStatus verificationStatus;
    private final Set<IncidentType> specialties;
    private final Set<String> regions;
    private final ProviderAvailabilityStatus availabilityStatus;
    private final PricingTier pricingTier;
    private final boolean active;
    private final int currentWorkload;
    private final int maxActiveCases;
    private final String notes;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ProviderProfileResponse(ProviderProfile providerProfile) {
        this.id = providerProfile.getId();
        this.userId = providerProfile.getUser() == null
                ? null
                : providerProfile.getUser().getId();
        this.userEmail = providerProfile.getUser() == null
                ? null
                : providerProfile.getUser().getEmail();
        this.providerType = providerProfile.getProviderType();
        this.displayName = providerProfile.getDisplayName();
        this.organizationName = providerProfile.getOrganizationName();
        this.email = providerProfile.getEmail();
        this.phone = providerProfile.getPhone();
        this.verificationStatus = providerProfile.getVerificationStatus();
        this.specialties = Set.copyOf(providerProfile.getSpecialties());
        this.regions = new TreeSet<>(providerProfile.getRegions());
        this.availabilityStatus = providerProfile.getAvailabilityStatus();
        this.pricingTier = providerProfile.getPricingTier();
        this.active = providerProfile.isActive();
        this.currentWorkload = providerProfile.getCurrentWorkload();
        this.maxActiveCases = providerProfile.getMaxActiveCases();
        this.notes = providerProfile.getNotes();
        this.createdAt = providerProfile.getCreatedAt();
        this.updatedAt = providerProfile.getUpdatedAt();
    }
}
