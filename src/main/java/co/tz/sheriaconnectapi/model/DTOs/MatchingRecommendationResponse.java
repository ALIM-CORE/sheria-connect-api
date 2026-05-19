package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import lombok.Getter;

@Getter
public class MatchingRecommendationResponse {
    private final ProviderProfileResponse providerProfile;
    private final int score;
    private final String scoreBreakdown;

    public MatchingRecommendationResponse(
            ProviderProfile providerProfile,
            int score,
            String scoreBreakdown
    ) {
        this.providerProfile = new ProviderProfileResponse(providerProfile);
        this.score = score;
        this.scoreBreakdown = scoreBreakdown;
    }
}
