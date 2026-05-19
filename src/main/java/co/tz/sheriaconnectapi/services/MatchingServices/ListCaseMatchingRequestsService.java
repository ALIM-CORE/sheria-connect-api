package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.repositories.CaseMatchRequestRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListCaseMatchingRequestsService
        implements Query<String, List<MatchingRequestResponse>> {

    private final IncidentReportRepository incidentReportRepository;
    private final CaseMatchRequestRepository caseMatchRequestRepository;

    public ListCaseMatchingRequestsService(
            IncidentReportRepository incidentReportRepository,
            CaseMatchRequestRepository caseMatchRequestRepository
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.caseMatchRequestRepository = caseMatchRequestRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<List<MatchingRequestResponse>>> execute(
            String caseNumber
    ) {
        IncidentReport report = incidentReportRepository.findByCaseNumber(caseNumber)
                .orElseThrow(IncidentReportNotFoundException::new);

        List<MatchingRequestResponse> matchingRequests = caseMatchRequestRepository
                .findByIncidentReportOrderByScoreDescCreatedAtAsc(report)
                .stream()
                .map(MatchingRequestResponse::new)
                .toList();

        return ResponseUtil.success(
                matchingRequests,
                "Matching requests retrieved successfully",
                HttpStatus.OK
        );
    }
}
