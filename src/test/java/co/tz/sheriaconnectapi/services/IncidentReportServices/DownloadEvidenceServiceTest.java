package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.UnauthorizedCaseAccessException;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import co.tz.sheriaconnectapi.repositories.EvidenceFileRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.services.MatchingServices.ProviderCaseRequestAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadEvidenceServiceTest {

    @TempDir
    private Path storageRoot;

    @Mock
    private IncidentReportRepository incidentReportRepository;
    @Mock
    private EvidenceFileRepository evidenceFileRepository;
    @Mock
    private IncidentReportAccessService accessService;
    @Mock
    private ProviderCaseRequestAccessService providerCaseRequestAccessService;
    @Mock
    private Authentication authentication;

    @Test
    void acceptedProviderCanDownloadEvidenceForMatchedCase() throws Exception {
        IncidentReport report = report();
        CaseMatchRequest request = request(report, MatchingRequestStatus.ACCEPTED);
        EvidenceFile evidence = evidence(report);
        Files.createDirectories(storageRoot.resolve("2026-08/SC-2608-ABC123"));
        Files.writeString(storageRoot.resolve(evidence.getRelativePath()), "sample evidence");

        when(providerCaseRequestAccessService.requireMyRequest(5L, authentication))
                .thenReturn(request);
        when(evidenceFileRepository.findByIdAndIncidentReport(9L, report))
                .thenReturn(Optional.of(evidence));

        var service = service();
        var response = service.downloadForProvider(5L, 9L, authentication);

        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("attachment"));
        assertEquals(15L, response.getHeaders().getContentLength());
    }

    @Test
    void pendingProviderRequestCannotDownloadEvidence() {
        IncidentReport report = report();
        CaseMatchRequest request = request(report, MatchingRequestStatus.REQUESTED);
        when(providerCaseRequestAccessService.requireMyRequest(5L, authentication))
                .thenReturn(request);

        assertThrows(
                UnauthorizedCaseAccessException.class,
                () -> service().downloadForProvider(5L, 9L, authentication)
        );
    }

    private DownloadEvidenceService service() {
        return new DownloadEvidenceService(
                incidentReportRepository,
                evidenceFileRepository,
                accessService,
                new EvidenceStorageService(storageRoot.toString(), 1024L, "application/pdf"),
                providerCaseRequestAccessService
        );
    }

    private IncidentReport report() {
        IncidentReport report = new IncidentReport();
        report.setId(3L);
        report.setCaseNumber("SC-2608-ABC123");
        return report;
    }

    private CaseMatchRequest request(
            IncidentReport report,
            MatchingRequestStatus status
    ) {
        CaseMatchRequest request = new CaseMatchRequest();
        request.setId(5L);
        request.setIncidentReport(report);
        request.setStatus(status);
        return request;
    }

    private EvidenceFile evidence(IncidentReport report) {
        EvidenceFile evidence = new EvidenceFile();
        evidence.setId(9L);
        evidence.setIncidentReport(report);
        evidence.setOriginalFileName("proof.pdf");
        evidence.setRelativePath("2026-08/SC-2608-ABC123/proof.pdf");
        evidence.setContentType("application/pdf");
        evidence.setFileSize(15L);
        return evidence;
    }
}
