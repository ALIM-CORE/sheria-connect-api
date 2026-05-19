package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Enums.LegalServiceProviderType;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, Long> {

    @Query("""
            SELECT DISTINCT p FROM ProviderProfile p
            LEFT JOIN FETCH p.specialties
            LEFT JOIN FETCH p.regions
            WHERE (:providerType IS NULL OR p.providerType = :providerType)
              AND (:verificationStatus IS NULL OR p.verificationStatus = :verificationStatus)
              AND (:availabilityStatus IS NULL OR p.availabilityStatus = :availabilityStatus)
              AND (:active IS NULL OR p.active = :active)
            ORDER BY p.displayName ASC
            """)
    List<ProviderProfile> search(
            @Param("providerType") LegalServiceProviderType providerType,
            @Param("verificationStatus") ProviderVerificationStatus verificationStatus,
            @Param("availabilityStatus") ProviderAvailabilityStatus availabilityStatus,
            @Param("active") Boolean active
    );

    @Query("""
            SELECT DISTINCT p FROM ProviderProfile p
            LEFT JOIN FETCH p.specialties
            LEFT JOIN FETCH p.regions
            WHERE p.active = true
              AND p.verificationStatus = :verificationStatus
              AND p.availabilityStatus IN :availabilityStatuses
            """)
    List<ProviderProfile> findAvailableVerifiedProfiles(
            @Param("verificationStatus") ProviderVerificationStatus verificationStatus,
            @Param("availabilityStatuses") Collection<ProviderAvailabilityStatus> availabilityStatuses
    );
}
