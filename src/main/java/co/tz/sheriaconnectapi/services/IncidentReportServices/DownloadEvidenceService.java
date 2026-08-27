package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.EvidenceFileNotFoundException;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.exceptions.UnauthorizedCaseAccessException;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import co.tz.sheriaconnectapi.repositories.EvidenceFileRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.services.MatchingServices.ProviderCaseRequestAccessService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Service
public class DownloadEvidenceService {
    private static final String INCIDENT_REPORT_READ = "INCIDENTREPORT_READ";

    private final IncidentReportRepository incidentReportRepository;
    private final EvidenceFileRepository evidenceFileRepository;
    private final IncidentReportAccessService accessService;
    private final EvidenceStorageService evidenceStorageService;
    private final ProviderCaseRequestAccessService providerCaseRequestAccessService;

    public DownloadEvidenceService(
            IncidentReportRepository incidentReportRepository,
            EvidenceFileRepository evidenceFileRepository,
            IncidentReportAccessService accessService,
            EvidenceStorageService evidenceStorageService,
            ProviderCaseRequestAccessService providerCaseRequestAccessService
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.evidenceFileRepository = evidenceFileRepository;
        this.accessService = accessService;
        this.evidenceStorageService = evidenceStorageService;
        this.providerCaseRequestAccessService = providerCaseRequestAccessService;
    }

    public ResponseEntity<Resource> downloadForIncident(
            String caseNumber,
            Long evidenceId,
            String trackingToken,
            Authentication authentication
    ) {
        IncidentReport report = incidentReportRepository.findByCaseNumber(caseNumber)
                .orElseThrow(IncidentReportNotFoundException::new);

        if (accessService.isStaffSession(authentication)) {
            if (!accessService.hasAuthority(authentication, INCIDENT_REPORT_READ)) {
                throw new UnauthorizedCaseAccessException();
            }
            accessService.assertStaffNotSelf(report, authentication);
        } else {
            accessService.assertCitizenAccess(report, authentication, trackingToken);
        }

        EvidenceFile evidenceFile = evidenceFileRepository
                .findByIdAndIncidentReport(evidenceId, report)
                .orElseThrow(EvidenceFileNotFoundException::new);

        return fileResponse(evidenceFile);
    }

    public ResponseEntity<Resource> downloadForProvider(
            Long matchingRequestId,
            Long evidenceId,
            Authentication authentication
    ) {
        CaseMatchRequest request = providerCaseRequestAccessService
                .requireMyRequest(matchingRequestId, authentication);
        if (request.getStatus() != MatchingRequestStatus.ACCEPTED) {
            throw new UnauthorizedCaseAccessException();
        }

        EvidenceFile evidenceFile = evidenceFileRepository
                .findByIdAndIncidentReport(evidenceId, request.getIncidentReport())
                .orElseThrow(EvidenceFileNotFoundException::new);

        return fileResponse(evidenceFile);
    }

    private ResponseEntity<Resource> fileResponse(EvidenceFile evidenceFile) {
        Path path = evidenceStorageService.resolveForDownload(evidenceFile);
        FileSystemResource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(contentType(evidenceFile.getContentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(evidenceFile.getOriginalFileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .contentLength(evidenceFile.getFileSize())
                .body(resource);
    }

    private MediaType contentType(String value) {
        if (value == null || value.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(value);
        } catch (RuntimeException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
