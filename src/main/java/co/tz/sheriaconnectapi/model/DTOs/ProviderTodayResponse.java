package co.tz.sheriaconnectapi.model.DTOs;

import java.util.List;

public record ProviderTodayResponse(
        ProviderProfileResponse providerProfile,
        ProviderTodayCountsResponse counts,
        MatchingRequestResponse nextRequest,
        List<MatchingRequestResponse> waitingQueue,
        ProviderResponseTimeResponse responseTime
) {
    public record ProviderTodayCountsResponse(
            int waitingRequests,
            int overdueRequests,
            int activeCases,
            long unreadNotifications,
            int currentWorkload,
            int maxActiveCases
    ) {
    }

    public record ProviderResponseTimeResponse(
            Long rollingAverageDecisionSeconds,
            Long previousRollingAverageDecisionSeconds,
            Long deltaSeconds,
            int decidedRequestCount,
            int previousDecidedRequestCount
    ) {
    }
}
