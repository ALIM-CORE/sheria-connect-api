package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import co.tz.sheriaconnectapi.model.Enums.PricingTier;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
public class ProviderMatchingScoreService {

    public ProviderMatchingScore score(
            IncidentReport report,
            ProviderProfile providerProfile
    ) {
        int score = 0;
        StringBuilder breakdown = new StringBuilder();

        if (providerProfile.getSpecialties().contains(report.getIncidentType())) {
            score += 45;
            breakdown.append("specialty:+45;");
        } else if (providerProfile.getSpecialties().isEmpty()) {
            score += 10;
            breakdown.append("generalist:+10;");
        }

        if (matchesRegion(report, providerProfile)) {
            score += 20;
            breakdown.append("region:+20;");
        } else if (providerProfile.getRegions().isEmpty()) {
            score += 8;
            breakdown.append("no-region-limit:+8;");
        }

        int urgencyScore = urgencyScore(report.getUrgency(), providerProfile.getAvailabilityStatus());
        score += urgencyScore;
        breakdown.append("urgency-fit:+").append(urgencyScore).append(";");

        int pricingScore = pricingScore(providerProfile.getPricingTier());
        score += pricingScore;
        breakdown.append("pricing:+").append(pricingScore).append(";");

        int workloadScore = workloadScore(providerProfile);
        score += workloadScore;
        breakdown.append("workload:+").append(workloadScore);

        return new ProviderMatchingScore(providerProfile, score, breakdown.toString());
    }

    public List<ProviderMatchingScore> topRecommendations(
            IncidentReport report,
            List<ProviderProfile> candidates
    ) {
        return candidates.stream()
                .map(providerProfile -> score(report, providerProfile))
                .sorted(
                        Comparator
                                .comparingInt(ProviderMatchingScore::score)
                                .reversed()
                                .thenComparing(match -> match.providerProfile().getId())
                )
                .limit(3)
                .toList();
    }

    private boolean matchesRegion(IncidentReport report, ProviderProfile providerProfile) {
        Set<String> reportPlaces = Stream.of(
                        report.getRegion(),
                        report.getDistrict(),
                        report.getWard()
                )
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalize)
                .collect(Collectors.toSet());

        if (reportPlaces.isEmpty()) {
            return false;
        }

        return providerProfile.getRegions().stream()
                .map(this::normalize)
                .anyMatch(reportPlaces::contains);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private int urgencyScore(
            IncidentUrgency urgency,
            ProviderAvailabilityStatus availabilityStatus
    ) {
        boolean urgent = urgency == IncidentUrgency.HIGH || urgency == IncidentUrgency.CRITICAL;
        if (availabilityStatus == ProviderAvailabilityStatus.AVAILABLE) {
            return urgent ? 15 : 10;
        }

        if (availabilityStatus == ProviderAvailabilityStatus.LIMITED) {
            return urgent ? 5 : 8;
        }

        return 0;
    }

    private int pricingScore(PricingTier pricingTier) {
        if (pricingTier == PricingTier.FREE) {
            return 10;
        }

        if (pricingTier == PricingTier.LOW_COST) {
            return 8;
        }

        if (pricingTier == PricingTier.STANDARD) {
            return 5;
        }

        return 1;
    }

    private int workloadScore(ProviderProfile providerProfile) {
        if (providerProfile.getMaxActiveCases() <= 0) {
            return 0;
        }

        double ratio = (double) providerProfile.getCurrentWorkload()
                / (double) providerProfile.getMaxActiveCases();

        if (ratio < 0.5) {
            return 10;
        }

        if (ratio < 0.8) {
            return 5;
        }

        return 0;
    }
}
