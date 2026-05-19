package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportLookupInput;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportResponse;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class GetIncidentReportService
        implements Query<IncidentReportLookupInput, IncidentReportResponse> {

    private final IncidentReportRepository incidentReportRepository;
    private final IncidentReportAccessService accessService;
    private final IncidentReportResponseFactory responseFactory;

    public GetIncidentReportService(
            IncidentReportRepository incidentReportRepository,
            IncidentReportAccessService accessService,
            IncidentReportResponseFactory responseFactory
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.accessService = accessService;
        this.responseFactory = responseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<IncidentReportResponse>> execute(
            IncidentReportLookupInput input
    ) {
        IncidentReport report = incidentReportRepository
                .findByCaseNumber(input.caseNumber())
                .orElseThrow(IncidentReportNotFoundException::new);

        if (!input.adminAccess()) {
            accessService.assertCitizenAccess(
                    report,
                    input.authentication(),
                    input.trackingToken()
            );
        }

        return ResponseUtil.success(
                responseFactory.build(report, null, input.adminAccess()),
                "Incident report retrieved successfully",
                HttpStatus.OK
        );
    }
}
