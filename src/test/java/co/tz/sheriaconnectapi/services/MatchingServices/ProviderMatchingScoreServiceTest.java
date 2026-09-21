package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import co.tz.sheriaconnectapi.model.Enums.PricingTier;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderMatchingScoreServiceTest {

    @Test
    void awardsSpecialtyPointsForADynamicCategoryCode() {
        IncidentReport report = new IncidentReport();
        report.setIncidentType("WORKPLACE_ABUSE");
        report.setUrgency(IncidentUrgency.MEDIUM);

        ProviderProfile provider = new ProviderProfile();
        provider.setSpecialties(Set.of("WORKPLACE_ABUSE"));
        provider.setAvailabilityStatus(ProviderAvailabilityStatus.AVAILABLE);
        provider.setPricingTier(PricingTier.FREE);
        provider.setMaxActiveCases(10);
        provider.setCurrentWorkload(0);

        ProviderMatchingScore score = new ProviderMatchingScoreService().score(report, provider);

        assertEquals(true, score.scoreBreakdown().contains("specialty:+45"));
    }
}
