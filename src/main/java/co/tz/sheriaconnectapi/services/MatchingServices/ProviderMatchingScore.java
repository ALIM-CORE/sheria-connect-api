package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;

public record ProviderMatchingScore(
        ProviderProfile providerProfile,
        int score,
        String scoreBreakdown
) {
}
