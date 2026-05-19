package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidMatchingRequestStatusException;
import co.tz.sheriaconnectapi.exceptions.MatchingRequestNotFoundException;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateMatchingRequestStatusInput;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
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

import java.util.Map;
import java.util.Set;

@Service
public class UpdateMatchingRequestStatusService
        implements Command<UpdateMatchingRequestStatusInput, MatchingRequestResponse> {

    private static final Map<MatchingRequestStatus, Set<MatchingRequestStatus>> TRANSITIONS =
            Map.of(
                    MatchingRequestStatus.REQUESTED,
                    Set.of(
                            MatchingRequestStatus.ACCEPTED,
                            MatchingRequestStatus.DECLINED,
                            MatchingRequestStatus.CANCELLED
                    ),
                    MatchingRequestStatus.ACCEPTED,
                    Set.of(
                            MatchingRequestStatus.COMPLETED,
                            MatchingRequestStatus.CANCELLED
                    )
            );

    private final CaseMatchRequestRepository caseMatchRequestRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final IncidentReportRepository incidentReportRepository;
    private final CaseStatusHistoryRepository caseStatusHistoryRepository;
    private final IncidentReportAccessService incidentReportAccessService;

    public UpdateMatchingRequestStatusService(
            CaseMatchRequestRepository caseMatchRequestRepository,
            ProviderProfileRepository providerProfileRepository,
            IncidentReportRepository incidentReportRepository,
            CaseStatusHistoryRepository caseStatusHistoryRepository,
            IncidentReportAccessService incidentReportAccessService
    ) {
        this.caseMatchRequestRepository = caseMatchRequestRepository;
        this.providerProfileRepository = providerProfileRepository;
        this.incidentReportRepository = incidentReportRepository;
        this.caseStatusHistoryRepository = caseStatusHistoryRepository;
        this.incidentReportAccessService = incidentReportAccessService;
    }

    @Override
    public ResponseEntity<StandardResponse<MatchingRequestResponse>> execute(
            UpdateMatchingRequestStatusInput input
    ) {
        if (input.request() == null || input.request().getStatus() == null) {
            throw new UserNotValidException("Matching status is required");
        }

        CaseMatchRequest matchRequest = caseMatchRequestRepository
                .findById(input.matchingRequestId())
                .orElseThrow(MatchingRequestNotFoundException::new);

        MatchingRequestStatus fromStatus = matchRequest.getStatus();
        MatchingRequestStatus toStatus = input.request().getStatus();
        if (!TRANSITIONS.getOrDefault(fromStatus, Set.of()).contains(toStatus)) {
            throw new InvalidMatchingRequestStatusException();
        }

        User decidedBy = incidentReportAccessService.requireAuthenticatedUser(input.authentication());
        ProviderProfile providerProfile = matchRequest.getProviderProfile();
        IncidentReport report = matchRequest.getIncidentReport();

        matchRequest.setStatus(toStatus);
        matchRequest.setDecidedBy(decidedBy);
        if (input.request().getNotes() != null && !input.request().getNotes().isBlank()) {
            matchRequest.setNotes(input.request().getNotes().trim());
        }

        if (toStatus == MatchingRequestStatus.ACCEPTED) {
            providerProfile.setCurrentWorkload(providerProfile.getCurrentWorkload() + 1);
            setReportStatus(report, IncidentReportStatus.MATCHED, decidedBy);
        }

        if (fromStatus == MatchingRequestStatus.ACCEPTED &&
                (toStatus == MatchingRequestStatus.COMPLETED ||
                        toStatus == MatchingRequestStatus.CANCELLED)) {
            providerProfile.setCurrentWorkload(
                    Math.max(providerProfile.getCurrentWorkload() - 1, 0)
            );
        }

        providerProfileRepository.save(providerProfile);
        incidentReportRepository.save(report);
        CaseMatchRequest saved = caseMatchRequestRepository.save(matchRequest);

        return ResponseUtil.success(
                new MatchingRequestResponse(saved),
                "Matching request updated successfully",
                HttpStatus.OK
        );
    }

    private void setReportStatus(
            IncidentReport report,
            IncidentReportStatus toStatus,
            User user
    ) {
        if (report.getStatus() == toStatus) {
            return;
        }

        IncidentReportStatus fromStatus = report.getStatus();
        report.setStatus(toStatus);

        CaseStatusHistory history = new CaseStatusHistory();
        history.setIncidentReport(report);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedByUser(user);
        history.setNote("Matching request accepted");
        caseStatusHistoryRepository.save(history);
    }
}
