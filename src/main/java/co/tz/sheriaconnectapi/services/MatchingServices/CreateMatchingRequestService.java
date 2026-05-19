package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.DuplicateMatchingRequestException;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.exceptions.ProviderProfileNotFoundException;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.CreateMatchingRequestInput;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import co.tz.sheriaconnectapi.repositories.CaseMatchRequestRepository;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.services.IncidentReportServices.IncidentReportAccessService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CreateMatchingRequestService
        implements Command<CreateMatchingRequestInput, MatchingRequestResponse> {

    private final IncidentReportRepository incidentReportRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final CaseMatchRequestRepository caseMatchRequestRepository;
    private final CaseStatusHistoryRepository caseStatusHistoryRepository;
    private final IncidentReportAccessService incidentReportAccessService;
    private final ProviderMatchingScoreService providerMatchingScoreService;

    public CreateMatchingRequestService(
            IncidentReportRepository incidentReportRepository,
            ProviderProfileRepository providerProfileRepository,
            CaseMatchRequestRepository caseMatchRequestRepository,
            CaseStatusHistoryRepository caseStatusHistoryRepository,
            IncidentReportAccessService incidentReportAccessService,
            ProviderMatchingScoreService providerMatchingScoreService
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.providerProfileRepository = providerProfileRepository;
        this.caseMatchRequestRepository = caseMatchRequestRepository;
        this.caseStatusHistoryRepository = caseStatusHistoryRepository;
        this.incidentReportAccessService = incidentReportAccessService;
        this.providerMatchingScoreService = providerMatchingScoreService;
    }

    @Override
    public ResponseEntity<StandardResponse<MatchingRequestResponse>> execute(
            CreateMatchingRequestInput input
    ) {
        if (input.request() == null || input.request().getProviderProfileId() == null) {
            throw new UserNotValidException("Provider profile is required");
        }

        IncidentReport report = incidentReportRepository.findByCaseNumber(input.caseNumber())
                .orElseThrow(IncidentReportNotFoundException::new);
        ProviderProfile providerProfile = providerProfileRepository
                .findById(input.request().getProviderProfileId())
                .orElseThrow(ProviderProfileNotFoundException::new);

        if (!providerProfile.isActive() ||
                providerProfile.getVerificationStatus() != ProviderVerificationStatus.VERIFIED ||
                providerProfile.getAvailabilityStatus() == ProviderAvailabilityStatus.UNAVAILABLE) {
            throw new UserNotValidException("Provider profile is not eligible for matching");
        }

        if (caseMatchRequestRepository.existsByIncidentReportAndProviderProfile(
                report,
                providerProfile
        )) {
            throw new DuplicateMatchingRequestException();
        }

        User requestedBy = incidentReportAccessService.requireAuthenticatedUser(input.authentication());
        ProviderMatchingScore score = providerMatchingScoreService.score(report, providerProfile);

        CaseMatchRequest matchRequest = new CaseMatchRequest();
        matchRequest.setIncidentReport(report);
        matchRequest.setProviderProfile(providerProfile);
        matchRequest.setStatus(MatchingRequestStatus.REQUESTED);
        matchRequest.setScore(score.score());
        matchRequest.setScoreBreakdown(score.scoreBreakdown());
        matchRequest.setRequestedBy(requestedBy);
        matchRequest.setNotes(trimToNull(input.request().getNotes()));

        report.setMatchingRequested(true);
        if (report.getStatus() == IncidentReportStatus.SUBMITTED ||
                report.getStatus() == IncidentReportStatus.UNDER_REVIEW) {
            IncidentReportStatus fromStatus = report.getStatus();
            report.setStatus(IncidentReportStatus.MATCHING_REQUESTED);
            createStatusHistory(
                    report,
                    fromStatus,
                    IncidentReportStatus.MATCHING_REQUESTED,
                    requestedBy,
                    "Matching request created"
            );
        }

        incidentReportRepository.save(report);
        CaseMatchRequest saved = caseMatchRequestRepository.save(matchRequest);

        return ResponseUtil.success(
                new MatchingRequestResponse(saved),
                "Matching request created successfully",
                HttpStatus.CREATED
        );
    }

    private void createStatusHistory(
            IncidentReport report,
            IncidentReportStatus fromStatus,
            IncidentReportStatus toStatus,
            User user,
            String note
    ) {
        CaseStatusHistory history = new CaseStatusHistory();
        history.setIncidentReport(report);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedByUser(user);
        history.setNote(note);
        caseStatusHistoryRepository.save(history);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
