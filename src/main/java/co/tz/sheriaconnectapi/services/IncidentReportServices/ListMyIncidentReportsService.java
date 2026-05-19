package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportSummaryResponse;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMyIncidentReportsService
        implements Query<Authentication, List<IncidentReportSummaryResponse>> {

    private final IncidentReportRepository incidentReportRepository;
    private final IncidentReportAccessService accessService;

    public ListMyIncidentReportsService(
            IncidentReportRepository incidentReportRepository,
            IncidentReportAccessService accessService
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.accessService = accessService;
    }

    @Override
    public ResponseEntity<StandardResponse<List<IncidentReportSummaryResponse>>> execute(
            Authentication authentication
    ) {
        User user = accessService.requireAuthenticatedUser(authentication);
        List<IncidentReportSummaryResponse> response = incidentReportRepository
                .findByReporterUserOrderByCreatedAtDesc(user)
                .stream()
                .map(IncidentReportSummaryResponse::new)
                .toList();

        return ResponseUtil.success(
                response,
                "Incident reports retrieved successfully",
                HttpStatus.OK
        );
    }
}
