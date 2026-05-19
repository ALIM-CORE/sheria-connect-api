package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.exceptions.InvalidCaseNumberException;
import co.tz.sheriaconnectapi.model.DTOs.AdminCaseNoteResponse;
import co.tz.sheriaconnectapi.model.DTOs.AdminIncidentReportSearchInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateAdminCaseNoteInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateAdminCaseNoteRequest;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportLookupInput;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportResponse;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportSummaryResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentReportStatusInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentReportStatusRequest;
import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.model.Enums.IncidentType;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import co.tz.sheriaconnectapi.services.IncidentReportServices.AdminListIncidentReportsService;
import co.tz.sheriaconnectapi.services.IncidentReportServices.CreateAdminCaseNoteService;
import co.tz.sheriaconnectapi.services.IncidentReportServices.GetIncidentReportService;
import co.tz.sheriaconnectapi.services.IncidentReportServices.UpdateIncidentReportStatusService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/admin/incident-reports")
public class AdminIncidentReportController {

    private static final Pattern CASE_NUMBER_PATTERN =
            Pattern.compile("^SC-\\d{4}-[A-Z2-9]{6}$");

    private final AdminListIncidentReportsService adminListIncidentReportsService;
    private final GetIncidentReportService getIncidentReportService;
    private final UpdateIncidentReportStatusService updateIncidentReportStatusService;
    private final CreateAdminCaseNoteService createAdminCaseNoteService;

    public AdminIncidentReportController(
            AdminListIncidentReportsService adminListIncidentReportsService,
            GetIncidentReportService getIncidentReportService,
            UpdateIncidentReportStatusService updateIncidentReportStatusService,
            CreateAdminCaseNoteService createAdminCaseNoteService
    ) {
        this.adminListIncidentReportsService = adminListIncidentReportsService;
        this.getIncidentReportService = getIncidentReportService;
        this.updateIncidentReportStatusService = updateIncidentReportStatusService;
        this.createAdminCaseNoteService = createAdminCaseNoteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INCIDENTREPORT_READ')")
    public ResponseEntity<StandardResponse<List<IncidentReportSummaryResponse>>> list(
            @RequestParam(required = false) IncidentReportStatus status,
            @RequestParam(required = false) IncidentUrgency urgency,
            @RequestParam(required = false) IncidentType incidentType,
            @RequestParam(required = false) AnonymityMode anonymityMode,
            @RequestParam(required = false) Boolean matchingRequested,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo
    ) {
        return adminListIncidentReportsService.execute(
                new AdminIncidentReportSearchInput(
                        status,
                        urgency,
                        incidentType,
                        anonymityMode,
                        matchingRequested,
                        createdFrom,
                        createdTo
                )
        );
    }

    @GetMapping("/{caseNumber}")
    @PreAuthorize("hasAuthority('INCIDENTREPORT_READ')")
    public ResponseEntity<StandardResponse<IncidentReportResponse>> get(
            @PathVariable String caseNumber,
            Authentication authentication
    ) {
        validateCaseNumber(caseNumber);
        return getIncidentReportService.execute(
                new IncidentReportLookupInput(caseNumber, null, authentication, true)
        );
    }

    @PatchMapping("/{caseNumber}/status")
    @PreAuthorize("hasAuthority('INCIDENTREPORT_UPDATE')")
    public ResponseEntity<StandardResponse<IncidentReportResponse>> updateStatus(
            @PathVariable String caseNumber,
            @RequestBody UpdateIncidentReportStatusRequest request,
            Authentication authentication
    ) {
        validateCaseNumber(caseNumber);
        return updateIncidentReportStatusService.execute(
                new UpdateIncidentReportStatusInput(caseNumber, request, authentication)
        );
    }

    @PostMapping("/{caseNumber}/notes")
    @PreAuthorize("hasAuthority('ADMINCASENOTE_CREATE')")
    public ResponseEntity<StandardResponse<AdminCaseNoteResponse>> addNote(
            @PathVariable String caseNumber,
            @RequestBody CreateAdminCaseNoteRequest request,
            Authentication authentication
    ) {
        validateCaseNumber(caseNumber);
        return createAdminCaseNoteService.execute(
                new CreateAdminCaseNoteInput(caseNumber, request, authentication)
        );
    }

    private void validateCaseNumber(String caseNumber) {
        if (caseNumber == null || !CASE_NUMBER_PATTERN.matcher(caseNumber).matches()) {
            throw new InvalidCaseNumberException();
        }
    }
}
