package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.exceptions.InvalidStatusTransitionException;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentReportStatusInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentReportStatusRequest;
import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class UpdateIncidentReportStatusService
        implements Command<UpdateIncidentReportStatusInput, IncidentReportResponse> {

    private static final Map<IncidentReportStatus, Set<IncidentReportStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    IncidentReportStatus.SUBMITTED,
                    Set.of(
                            IncidentReportStatus.UNDER_REVIEW,
                            IncidentReportStatus.NEEDS_INFO,
                            IncidentReportStatus.MATCHING_REQUESTED,
                            IncidentReportStatus.REJECTED,
                            IncidentReportStatus.CLOSED
                    ),
                    IncidentReportStatus.UNDER_REVIEW,
                    Set.of(
                            IncidentReportStatus.NEEDS_INFO,
                            IncidentReportStatus.MATCHING_REQUESTED,
                            IncidentReportStatus.MATCHED,
                            IncidentReportStatus.IN_PROGRESS,
                            IncidentReportStatus.REJECTED,
                            IncidentReportStatus.CLOSED
                    ),
                    IncidentReportStatus.NEEDS_INFO,
                    Set.of(
                            IncidentReportStatus.UNDER_REVIEW,
                            IncidentReportStatus.REJECTED,
                            IncidentReportStatus.CLOSED
                    ),
                    IncidentReportStatus.MATCHING_REQUESTED,
                    Set.of(
                            IncidentReportStatus.MATCHED,
                            IncidentReportStatus.UNDER_REVIEW,
                            IncidentReportStatus.REJECTED,
                            IncidentReportStatus.CLOSED
                    ),
                    IncidentReportStatus.MATCHED,
                    Set.of(
                            IncidentReportStatus.IN_PROGRESS,
                            IncidentReportStatus.RESOLVED,
                            IncidentReportStatus.CLOSED
                    ),
                    IncidentReportStatus.IN_PROGRESS,
                    Set.of(
                            IncidentReportStatus.RESOLVED,
                            IncidentReportStatus.CLOSED
                    ),
                    IncidentReportStatus.RESOLVED,
                    Set.of(IncidentReportStatus.CLOSED),
                    IncidentReportStatus.CLOSED,
                    Set.of(),
                    IncidentReportStatus.REJECTED,
                    Set.of()
            );

    private final IncidentReportRepository incidentReportRepository;
    private final CaseStatusHistoryRepository caseStatusHistoryRepository;
    private final IncidentReportAccessService accessService;
    private final IncidentReportResponseFactory responseFactory;

    public UpdateIncidentReportStatusService(
            IncidentReportRepository incidentReportRepository,
            CaseStatusHistoryRepository caseStatusHistoryRepository,
            IncidentReportAccessService accessService,
            IncidentReportResponseFactory responseFactory
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.caseStatusHistoryRepository = caseStatusHistoryRepository;
        this.accessService = accessService;
        this.responseFactory = responseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<IncidentReportResponse>> execute(
            UpdateIncidentReportStatusInput input
    ) {
        UpdateIncidentReportStatusRequest request = input.request();
        if (request == null || request.getStatus() == null) {
            throw new UserNotValidException("Case status is required");
        }

        IncidentReport report = incidentReportRepository
                .findByCaseNumber(input.caseNumber())
                .orElseThrow(IncidentReportNotFoundException::new);

        IncidentReportStatus oldStatus = report.getStatus();
        IncidentReportStatus newStatus = request.getStatus();
        if (oldStatus != newStatus) {
            if (!ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of()).contains(newStatus)) {
                throw new InvalidStatusTransitionException();
            }

            report.setStatus(newStatus);
            report = incidentReportRepository.save(report);

            User adminUser = accessService.authenticatedUser(input.authentication())
                    .orElse(null);

            CaseStatusHistory history = new CaseStatusHistory();
            history.setIncidentReport(report);
            history.setFromStatus(oldStatus);
            history.setToStatus(newStatus);
            history.setChangedByUser(adminUser);
            history.setNote(trimToNull(request.getNote()));
            caseStatusHistoryRepository.save(history);
        }

        return ResponseUtil.success(
                responseFactory.build(report, null, true),
                "Incident report status updated successfully",
                HttpStatus.OK
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
