package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.exceptions.InvalidCaseNumberException;
import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentReportInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentReportRequest;
import co.tz.sheriaconnectapi.model.DTOs.EvidenceFileResponse;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportLookupInput;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportResponse;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportSummaryResponse;
import co.tz.sheriaconnectapi.model.DTOs.UploadEvidenceInput;
import co.tz.sheriaconnectapi.model.Enums.EvidenceUploadSource;
import co.tz.sheriaconnectapi.services.IncidentReportServices.CreateIncidentReportService;
import co.tz.sheriaconnectapi.services.IncidentReportServices.GetIncidentReportService;
import co.tz.sheriaconnectapi.services.IncidentReportServices.ListMyIncidentReportsService;
import co.tz.sheriaconnectapi.services.IncidentReportServices.UploadEvidenceService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/incident-reports")
public class IncidentReportController {

    private static final Pattern CASE_NUMBER_PATTERN =
            Pattern.compile("^SC-\\d{4}-[A-Z2-9]{6}$");

    private final CreateIncidentReportService createIncidentReportService;
    private final GetIncidentReportService getIncidentReportService;
    private final ListMyIncidentReportsService listMyIncidentReportsService;
    private final UploadEvidenceService uploadEvidenceService;

    public IncidentReportController(
            CreateIncidentReportService createIncidentReportService,
            GetIncidentReportService getIncidentReportService,
            ListMyIncidentReportsService listMyIncidentReportsService,
            UploadEvidenceService uploadEvidenceService
    ) {
        this.createIncidentReportService = createIncidentReportService;
        this.getIncidentReportService = getIncidentReportService;
        this.listMyIncidentReportsService = listMyIncidentReportsService;
        this.uploadEvidenceService = uploadEvidenceService;
    }

    @PostMapping
    public ResponseEntity<StandardResponse<IncidentReportResponse>> create(
            @RequestBody CreateIncidentReportRequest request,
            Authentication authentication
    ) {
        return createIncidentReportService.execute(
                new CreateIncidentReportInput(request, authentication)
        );
    }

    @GetMapping("/mine")
    public ResponseEntity<StandardResponse<List<IncidentReportSummaryResponse>>> mine(
            Authentication authentication
    ) {
        return listMyIncidentReportsService.execute(authentication);
    }

    @GetMapping("/{caseNumber}")
    public ResponseEntity<StandardResponse<IncidentReportResponse>> get(
            @PathVariable String caseNumber,
            @RequestParam(required = false) String trackingToken,
            @RequestHeader(name = "X-Case-Tracking-Token", required = false) String trackingTokenHeader,
            Authentication authentication
    ) {
        validateCaseNumber(caseNumber);
        return getIncidentReportService.execute(
                new IncidentReportLookupInput(
                        caseNumber,
                        firstPresent(trackingTokenHeader, trackingToken),
                        authentication,
                        false
                )
        );
    }

    @PostMapping(
            value = "/{caseNumber}/evidence",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StandardResponse<EvidenceFileResponse>> uploadEvidence(
            @PathVariable String caseNumber,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String trackingToken,
            @RequestHeader(name = "X-Case-Tracking-Token", required = false) String trackingTokenHeader,
            @RequestParam(required = false) EvidenceUploadSource uploadSource,
            Authentication authentication
    ) {
        validateCaseNumber(caseNumber);
        return uploadEvidenceService.execute(
                new UploadEvidenceInput(
                        caseNumber,
                        firstPresent(trackingTokenHeader, trackingToken),
                        file,
                        uploadSource,
                        authentication
                )
        );
    }

    private void validateCaseNumber(String caseNumber) {
        if (caseNumber == null || !CASE_NUMBER_PATTERN.matcher(caseNumber).matches()) {
            throw new InvalidCaseNumberException();
        }
    }

    private String firstPresent(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) return preferred;
        return fallback;
    }
}
