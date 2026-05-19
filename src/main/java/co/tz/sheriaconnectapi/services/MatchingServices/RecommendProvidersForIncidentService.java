package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRecommendationResponse;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendProvidersForIncidentService
        implements Query<String, List<MatchingRecommendationResponse>> {

    private final IncidentReportRepository incidentReportRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderMatchingScoreService providerMatchingScoreService;

    public RecommendProvidersForIncidentService(
            IncidentReportRepository incidentReportRepository,
            ProviderProfileRepository providerProfileRepository,
            ProviderMatchingScoreService providerMatchingScoreService
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.providerProfileRepository = providerProfileRepository;
        this.providerMatchingScoreService = providerMatchingScoreService;
    }

    @Override
    public ResponseEntity<StandardResponse<List<MatchingRecommendationResponse>>> execute(
            String caseNumber
    ) {
        IncidentReport report = incidentReportRepository.findByCaseNumber(caseNumber)
                .orElseThrow(IncidentReportNotFoundException::new);

        List<ProviderProfile> candidates = providerProfileRepository.findAvailableVerifiedProfiles(
                ProviderVerificationStatus.VERIFIED,
                List.of(
                        ProviderAvailabilityStatus.AVAILABLE,
                        ProviderAvailabilityStatus.LIMITED
                )
        );

        List<MatchingRecommendationResponse> recommendations = providerMatchingScoreService
                .topRecommendations(report, candidates)
                .stream()
                .map(score -> new MatchingRecommendationResponse(
                        score.providerProfile(),
                        score.score(),
                        score.scoreBreakdown()
                ))
                .toList();

        return ResponseUtil.success(
                recommendations,
                "Provider recommendations retrieved successfully",
                HttpStatus.OK
        );
    }
}
