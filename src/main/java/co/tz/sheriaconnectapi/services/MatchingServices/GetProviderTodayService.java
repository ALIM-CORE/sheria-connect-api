package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileResponse;
import co.tz.sheriaconnectapi.model.DTOs.ProviderTodayResponse;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import co.tz.sheriaconnectapi.repositories.CaseMatchRequestRepository;
import co.tz.sheriaconnectapi.repositories.UserNotificationRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class GetProviderTodayService implements Query<Authentication, ProviderTodayResponse> {
    private static final Duration OVERDUE_AFTER = Duration.ofHours(24);
    private static final Duration RESPONSE_TIME_WINDOW = Duration.ofDays(30);
    private static final int WAITING_QUEUE_LIMIT = 5;

    private final ProviderCaseRequestAccessService accessService;
    private final CaseMatchRequestRepository caseMatchRequestRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ProviderMatchingRequestResponseFactory responseFactory;

    public GetProviderTodayService(
            ProviderCaseRequestAccessService accessService,
            CaseMatchRequestRepository caseMatchRequestRepository,
            UserNotificationRepository userNotificationRepository,
            ProviderMatchingRequestResponseFactory responseFactory
    ) {
        this.accessService = accessService;
        this.caseMatchRequestRepository = caseMatchRequestRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.responseFactory = responseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<ProviderTodayResponse>> execute(
            Authentication authentication
    ) {
        ProviderProfile profile = accessService.requireMyProviderProfile(authentication);
        List<CaseMatchRequest> requests = caseMatchRequestRepository
                .findByProviderProfileOrderByCreatedAtDesc(profile);
        Instant now = Instant.now();

        List<CaseMatchRequest> waitingRequests = requests.stream()
                .filter(request -> request.getStatus() == MatchingRequestStatus.REQUESTED)
                .sorted(triageOrder())
                .toList();

        int activeCases = (int) requests.stream()
                .filter(request -> request.getStatus() == MatchingRequestStatus.ACCEPTED)
                .count();
        int overdueRequests = (int) waitingRequests.stream()
                .filter(request -> isOverdue(request, now))
                .count();
        long unreadNotifications = profile.getUser() == null
                ? 0
                : userNotificationRepository.countByUserAndContextAndReadAtIsNull(
                        profile.getUser(),
                        AccessContext.PROVIDER
                );

        List<MatchingRequestResponse> waitingQueue = waitingRequests.stream()
                .limit(WAITING_QUEUE_LIMIT)
                .map(responseFactory::from)
                .toList();
        MatchingRequestResponse nextRequest = waitingQueue.isEmpty()
                ? null
                : waitingQueue.get(0);

        ProviderTodayResponse body = new ProviderTodayResponse(
                new ProviderProfileResponse(profile),
                new ProviderTodayResponse.ProviderTodayCountsResponse(
                        waitingRequests.size(),
                        overdueRequests,
                        activeCases,
                        unreadNotifications,
                        profile.getCurrentWorkload(),
                        profile.getMaxActiveCases()
                ),
                nextRequest,
                waitingQueue,
                responseTime(requests, now)
        );

        return ResponseUtil.success(body, "Provider today summary retrieved", HttpStatus.OK);
    }

    private Comparator<CaseMatchRequest> triageOrder() {
        return Comparator
                .comparingInt((CaseMatchRequest request) -> urgencyRank(request.getIncidentReport().getUrgency()))
                .thenComparing(
                        CaseMatchRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
    }

    private int urgencyRank(IncidentUrgency urgency) {
        if (urgency == IncidentUrgency.CRITICAL) {
            return 0;
        }
        if (urgency == IncidentUrgency.HIGH) {
            return 1;
        }
        if (urgency == IncidentUrgency.MEDIUM) {
            return 2;
        }
        return 3;
    }

    private boolean isOverdue(CaseMatchRequest request, Instant now) {
        return request.getCreatedAt() != null
                && request.getCreatedAt().isBefore(now.minus(OVERDUE_AFTER));
    }

    private ProviderTodayResponse.ProviderResponseTimeResponse responseTime(
            List<CaseMatchRequest> requests,
            Instant now
    ) {
        Instant currentWindowStart = now.minus(RESPONSE_TIME_WINDOW);
        Instant previousWindowStart = currentWindowStart.minus(RESPONSE_TIME_WINDOW);

        List<Long> currentDurations = decisionDurationsBetween(
                requests,
                currentWindowStart,
                now
        );
        List<Long> previousDurations = decisionDurationsBetween(
                requests,
                previousWindowStart,
                currentWindowStart
        );

        Long currentAverage = averageSeconds(currentDurations);
        Long previousAverage = averageSeconds(previousDurations);
        Long delta = currentAverage == null || previousAverage == null
                ? null
                : currentAverage - previousAverage;

        return new ProviderTodayResponse.ProviderResponseTimeResponse(
                currentAverage,
                previousAverage,
                delta,
                currentDurations.size(),
                previousDurations.size()
        );
    }

    private List<Long> decisionDurationsBetween(
            List<CaseMatchRequest> requests,
            Instant startInclusive,
            Instant endExclusive
    ) {
        return requests.stream()
                .filter(this::isDecision)
                .filter(request -> request.getCreatedAt() != null && request.getUpdatedAt() != null)
                .filter(request -> !request.getUpdatedAt().isBefore(startInclusive))
                .filter(request -> request.getUpdatedAt().isBefore(endExclusive))
                .map(request -> Duration.between(request.getCreatedAt(), request.getUpdatedAt()).toSeconds())
                .filter(seconds -> seconds >= 0)
                .toList();
    }

    private boolean isDecision(CaseMatchRequest request) {
        return request.getStatus() == MatchingRequestStatus.ACCEPTED
                || request.getStatus() == MatchingRequestStatus.DECLINED;
    }

    private Long averageSeconds(List<Long> seconds) {
        return seconds.stream()
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .stream()
                .mapToLong(Math::round)
                .boxed()
                .findFirst()
                .orElse(null);
    }
}
