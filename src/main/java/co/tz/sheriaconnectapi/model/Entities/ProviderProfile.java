package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.IncidentType;
import co.tz.sheriaconnectapi.model.Enums.LegalServiceProviderType;
import co.tz.sheriaconnectapi.model.Enums.PricingTier;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "provider_profiles",
        indexes = {
                @Index(name = "idx_provider_profiles_verification", columnList = "verification_status"),
                @Index(name = "idx_provider_profiles_availability", columnList = "availability_status"),
                @Index(name = "idx_provider_profiles_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProviderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    private LegalServiceProviderType providerType;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @Column(name = "organization_name", length = 180)
    private String organizationName;

    @Column(length = 180)
    private String email;

    @Column(length = 80)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private ProviderVerificationStatus verificationStatus = ProviderVerificationStatus.PENDING;

    @ElementCollection(targetClass = IncidentType.class)
    @CollectionTable(
            name = "provider_profile_specialties",
            joinColumns = @JoinColumn(name = "provider_profile_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 64)
    private Set<IncidentType> specialties = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "provider_profile_regions",
            joinColumns = @JoinColumn(name = "provider_profile_id")
    )
    @Column(name = "region", nullable = false, length = 120)
    private Set<String> regions = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 32)
    private ProviderAvailabilityStatus availabilityStatus = ProviderAvailabilityStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_tier", nullable = false, length = 32)
    private PricingTier pricingTier = PricingTier.FREE;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "current_workload", nullable = false)
    private int currentWorkload = 0;

    @Column(name = "max_active_cases", nullable = false)
    private int maxActiveCases = 10;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
