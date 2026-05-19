package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentReportInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentReportRequest;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportResponse;
import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CreateIncidentReportService
        implements Command<CreateIncidentReportInput, IncidentReportResponse> {

    private final IncidentReportRepository incidentReportRepository;
    private final CaseStatusHistoryRepository caseStatusHistoryRepository;
    private final CaseNumberGeneratorService caseNumberGeneratorService;
    private final TrackingTokenService trackingTokenService;
    private final IncidentReportAccessService accessService;
    private final IncidentReportResponseFactory responseFactory;

    public CreateIncidentReportService(
            IncidentReportRepository incidentReportRepository,
            CaseStatusHistoryRepository caseStatusHistoryRepository,
            CaseNumberGeneratorService caseNumberGeneratorService,
            TrackingTokenService trackingTokenService,
            IncidentReportAccessService accessService,
            IncidentReportResponseFactory responseFactory
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.caseStatusHistoryRepository = caseStatusHistoryRepository;
        this.caseNumberGeneratorService = caseNumberGeneratorService;
        this.trackingTokenService = trackingTokenService;
        this.accessService = accessService;
        this.responseFactory = responseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<IncidentReportResponse>> execute(
            CreateIncidentReportInput input
    ) {
        CreateIncidentReportRequest request = input.request();
        validate(request);

        Optional<User> reporter = accessService.authenticatedUser(input.authentication());
        String trackingToken = reporter.isPresent()
                ? null
                : trackingTokenService.generateToken();

        IncidentReport report = new IncidentReport();
        report.setCaseNumber(caseNumberGeneratorService.generate());
        report.setReporterUser(reporter.orElse(null));
        report.setTrackingTokenHash(trackingToken == null
                ? null
                : trackingTokenService.hash(trackingToken));
        report.setAnonymityMode(request.getAnonymityMode());
        report.setIncidentType(request.getIncidentType());
        report.setUrgency(request.getUrgency());
        report.setStatus(IncidentReportStatus.SUBMITTED);
        report.setTitle(trimToNull(request.getTitle()));
        report.setDescription(request.getDescription().trim());
        report.setIncidentDate(request.getIncidentDate());
        report.setLocationDescription(trimToNull(request.getLocationDescription()));
        report.setRegion(trimToNull(request.getRegion()));
        report.setDistrict(trimToNull(request.getDistrict()));
        report.setWard(trimToNull(request.getWard()));
        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());
        report.setPseudonym(trimToNull(request.getPseudonym()));
        report.setContactName(trimToNull(request.getContactName()));
        report.setContactEmail(trimToNull(request.getContactEmail()));
        report.setContactPhone(trimToNull(request.getContactPhone()));
        report.setMatchingRequested(Boolean.TRUE.equals(request.getMatchingRequested()));

        report = incidentReportRepository.save(report);

        CaseStatusHistory history = new CaseStatusHistory();
        history.setIncidentReport(report);
        history.setToStatus(IncidentReportStatus.SUBMITTED);
        history.setNote("Incident report submitted");
        caseStatusHistoryRepository.save(history);

        IncidentReportResponse response =
                responseFactory.build(report, trackingToken, false);

        return ResponseUtil.success(
                response,
                "Incident report submitted successfully",
                HttpStatus.CREATED
        );
    }

    private void validate(CreateIncidentReportRequest request) {
        if (request == null) {
            throw new UserNotValidException("Incident report payload is required");
        }
        if (request.getAnonymityMode() == null) {
            throw new UserNotValidException("Anonymity mode is required");
        }
        if (request.getIncidentType() == null) {
            throw new UserNotValidException("Incident type is required");
        }
        if (request.getUrgency() == null) {
            throw new UserNotValidException("Incident urgency is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new UserNotValidException("Incident description is required");
        }
        if (request.getAnonymityMode() == AnonymityMode.PSEUDONYMOUS
                && (request.getPseudonym() == null || request.getPseudonym().isBlank())) {
            throw new UserNotValidException("Pseudonym is required for pseudonymous reports");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
