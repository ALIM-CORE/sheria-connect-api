package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import co.tz.sheriaconnectapi.repositories.CaseMatchRequestRepository;
import co.tz.sheriaconnectapi.repositories.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProviderTodayServiceTest {

    @Mock
    private ProviderCaseRequestAccessService accessService;
    @Mock
    private CaseMatchRequestRepository caseMatchRequestRepository;
    @Mock
    private UserNotificationRepository userNotificationRepository;
    @Mock
    private ProviderMatchingRequestResponseFactory responseFactory;
    @Mock
    private Authentication authentication;

    @Test
    void todaySummaryPromotesUrgentOldestRequestAndCountsWork() {
        Instant now = Instant.now();
        User user = new User();
        user.setId(11L);

        ProviderProfile profile = new ProviderProfile();
        profile.setId(7L);
        profile.setUser(user);
        profile.setCurrentWorkload(2);
        profile.setMaxActiveCases(5);

        CaseMatchRequest criticalOlder = request(
                1L,
                profile,
                MatchingRequestStatus.REQUESTED,
                IncidentUrgency.CRITICAL,
                now.minus(Durations.hours(30)),
                now.minus(Durations.hours(30))
        );
        CaseMatchRequest criticalNewer = request(
                2L,
                profile,
                MatchingRequestStatus.REQUESTED,
                IncidentUrgency.CRITICAL,
                now.minus(Durations.hours(3)),
                now.minus(Durations.hours(3))
        );
        CaseMatchRequest highOldest = request(
                3L,
                profile,
                MatchingRequestStatus.REQUESTED,
                IncidentUrgency.HIGH,
                now.minus(Durations.days(5)),
                now.minus(Durations.days(5))
        );
        CaseMatchRequest accepted = request(
                4L,
                profile,
                MatchingRequestStatus.ACCEPTED,
                IncidentUrgency.MEDIUM,
                now.minus(Durations.hours(5)),
                now.minus(Durations.hours(1))
        );
        CaseMatchRequest previousDeclined = request(
                5L,
                profile,
                MatchingRequestStatus.DECLINED,
                IncidentUrgency.LOW,
                now.minus(Durations.days(45)),
                now.minus(Durations.days(44))
        );

        MatchingRequestResponse criticalOlderResponse = mock(MatchingRequestResponse.class);
        MatchingRequestResponse criticalNewerResponse = mock(MatchingRequestResponse.class);
        MatchingRequestResponse highOldestResponse = mock(MatchingRequestResponse.class);

        when(accessService.requireMyProviderProfile(authentication)).thenReturn(profile);
        when(caseMatchRequestRepository.findByProviderProfileOrderByCreatedAtDesc(profile))
                .thenReturn(List.of(criticalNewer, highOldest, accepted, criticalOlder, previousDeclined));
        when(userNotificationRepository.countByUserAndContextAndReadAtIsNull(user, AccessContext.PROVIDER))
                .thenReturn(2L);
        when(responseFactory.from(criticalOlder)).thenReturn(criticalOlderResponse);
        when(responseFactory.from(criticalNewer)).thenReturn(criticalNewerResponse);
        when(responseFactory.from(highOldest)).thenReturn(highOldestResponse);

        var service = new GetProviderTodayService(
                accessService,
                caseMatchRequestRepository,
                userNotificationRepository,
                responseFactory
        );

        var response = service.execute(authentication);
        var body = response.getBody().getBody();

        assertEquals(3, body.counts().waitingRequests());
        assertEquals(2, body.counts().overdueRequests());
        assertEquals(1, body.counts().activeCases());
        assertEquals(2L, body.counts().unreadNotifications());
        assertEquals(2, body.counts().currentWorkload());
        assertEquals(5, body.counts().maxActiveCases());
        assertSame(criticalOlderResponse, body.nextRequest());
        assertEquals(List.of(criticalOlderResponse, criticalNewerResponse, highOldestResponse), body.waitingQueue());
        assertEquals(14_400L, body.responseTime().rollingAverageDecisionSeconds());
        assertEquals(86_400L, body.responseTime().previousRollingAverageDecisionSeconds());
        assertEquals(-72_000L, body.responseTime().deltaSeconds());
    }

    private CaseMatchRequest request(
            Long id,
            ProviderProfile profile,
            MatchingRequestStatus status,
            IncidentUrgency urgency,
            Instant createdAt,
            Instant updatedAt
    ) {
        IncidentReport report = new IncidentReport();
        report.setCaseNumber("SC-2608-" + id);
        report.setUrgency(urgency);

        CaseMatchRequest request = new CaseMatchRequest();
        request.setId(id);
        request.setProviderProfile(profile);
        request.setIncidentReport(report);
        request.setStatus(status);
        request.setCreatedAt(createdAt);
        request.setUpdatedAt(updatedAt);
        return request;
    }

    private static final class Durations {
        private Durations() {
        }

        static java.time.Duration hours(long hours) {
            return java.time.Duration.ofHours(hours);
        }

        static java.time.Duration days(long days) {
            return java.time.Duration.ofDays(days);
        }
    }
}
