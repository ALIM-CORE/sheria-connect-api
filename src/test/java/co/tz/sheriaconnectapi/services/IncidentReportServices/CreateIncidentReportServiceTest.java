package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.ReportSubmissionIdReusedException;
import co.tz.sheriaconnectapi.exceptions.ReportSubmissionTokenUnavailableException;
import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentReportInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentReportRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.services.IncidentCategoryServices.IncidentCategoryValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateIncidentReportServiceTest {

    @Mock
    private IncidentReportRepository reportRepository;
    @Mock
    private CaseStatusHistoryRepository historyRepository;
    @Mock
    private CaseNumberGeneratorService caseNumberGeneratorService;
    @Mock
    private TrackingTokenService trackingTokenService;
    @Mock
    private IncidentReportAccessService accessService;
    @Mock
    private IncidentReportResponseFactory responseFactory;
    @Mock
    private IncidentCategoryValidationService categoryValidationService;
    @Mock
    private IncidentReportSubmissionFingerprintService fingerprintService;
    @Mock
    private IncidentReportSubmissionLockService lockService;

    @Test
    void firstGuestSubmissionStoresIdFingerprintAndDerivedTokenHash() {
        UUID submissionId = UUID.randomUUID();
        CreateIncidentReportRequest request = request(submissionId, "Original details");
        when(accessService.authenticatedCitizenUser(null)).thenReturn(Optional.empty());
        when(fingerprintService.fingerprint(request)).thenReturn("fingerprint");
        when(reportRepository.findBySubmissionId(submissionId)).thenReturn(Optional.empty());
        when(trackingTokenService.deriveToken(submissionId)).thenReturn("ABCD-EFGH-JKMN-PQRS-TVWX");
        when(trackingTokenService.hash("ABCD-EFGH-JKMN-PQRS-TVWX")).thenReturn("token-hash");
        when(categoryValidationService.requireSelectable("GENDER_BASED_VIOLENCE"))
                .thenReturn("GENDER_BASED_VIOLENCE");
        when(caseNumberGeneratorService.generate()).thenReturn("SC-2609-ABC234");
        when(reportRepository.save(any(IncidentReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service().execute(new CreateIncidentReportInput(request, null));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(lockService).lock(submissionId);
        ArgumentCaptor<IncidentReport> reportCaptor = ArgumentCaptor.forClass(IncidentReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        IncidentReport saved = reportCaptor.getValue();
        assertEquals(submissionId, saved.getSubmissionId());
        assertEquals("fingerprint", saved.getSubmissionFingerprint());
        assertEquals("token-hash", saved.getTrackingTokenHash());
        verify(responseFactory).build(saved, "ABCD-EFGH-JKMN-PQRS-TVWX", false);
    }

    @Test
    void matchingReplayReturnsOriginalGuestReportAndToken() {
        UUID submissionId = UUID.randomUUID();
        CreateIncidentReportRequest request = request(submissionId, "Original details");
        IncidentReport existing = existingGuest(submissionId, "fingerprint", "token-hash");
        when(accessService.authenticatedCitizenUser(null)).thenReturn(Optional.empty());
        when(fingerprintService.fingerprint(request)).thenReturn("fingerprint");
        when(reportRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(existing));
        when(trackingTokenService.deriveToken(submissionId)).thenReturn("ABCD-EFGH-JKMN-PQRS-TVWX");
        when(trackingTokenService.matches("ABCD-EFGH-JKMN-PQRS-TVWX", "token-hash"))
                .thenReturn(true);

        var response = service().execute(new CreateIncidentReportInput(request, null));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(responseFactory).build(existing, "ABCD-EFGH-JKMN-PQRS-TVWX", false);
        verify(reportRepository, never()).save(any());
        verify(categoryValidationService, never()).requireSelectable(any());
    }

    @Test
    void reusedIdWithDifferentPayloadIsRejected() {
        UUID submissionId = UUID.randomUUID();
        CreateIncidentReportRequest request = request(submissionId, "Changed details");
        IncidentReport existing = existingGuest(submissionId, "original-fingerprint", "token-hash");
        when(accessService.authenticatedCitizenUser(null)).thenReturn(Optional.empty());
        when(fingerprintService.fingerprint(request)).thenReturn("changed-fingerprint");
        when(reportRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(existing));

        assertThrows(
                ReportSubmissionIdReusedException.class,
                () -> service().execute(new CreateIncidentReportInput(request, null))
        );
    }

    @Test
    void reusedIdFromDifferentIdentityIsRejected() {
        UUID submissionId = UUID.randomUUID();
        CreateIncidentReportRequest request = request(submissionId, "Original details");
        User owner = user(7L);
        User otherUser = user(8L);
        IncidentReport existing = existingGuest(submissionId, "fingerprint", null);
        existing.setReporterUser(owner);
        when(accessService.authenticatedCitizenUser(null)).thenReturn(Optional.of(otherUser));
        when(fingerprintService.fingerprint(request)).thenReturn("fingerprint");
        when(reportRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(existing));

        assertThrows(
                ReportSubmissionIdReusedException.class,
                () -> service().execute(new CreateIncidentReportInput(request, null))
        );
    }

    @Test
    void replayFailsRatherThanReturningTokenAfterDerivationKeyChanges() {
        UUID submissionId = UUID.randomUUID();
        CreateIncidentReportRequest request = request(submissionId, "Original details");
        IncidentReport existing = existingGuest(submissionId, "fingerprint", "original-hash");
        when(accessService.authenticatedCitizenUser(null)).thenReturn(Optional.empty());
        when(fingerprintService.fingerprint(request)).thenReturn("fingerprint");
        when(reportRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(existing));
        when(trackingTokenService.deriveToken(submissionId)).thenReturn("DIFF-EREN-TKEY-TOKE-N234");
        when(trackingTokenService.matches("DIFF-EREN-TKEY-TOKE-N234", "original-hash"))
                .thenReturn(false);

        assertThrows(
                ReportSubmissionTokenUnavailableException.class,
                () -> service().execute(new CreateIncidentReportInput(request, null))
        );
    }

    @Test
    void legacySubmissionWithoutIdStillCreatesReportWithRandomShortToken() {
        CreateIncidentReportRequest request = request(null, "Legacy client details");
        when(accessService.authenticatedCitizenUser(null)).thenReturn(Optional.empty());
        when(fingerprintService.fingerprint(request)).thenReturn("unused-fingerprint");
        when(trackingTokenService.generateToken()).thenReturn("ABCD-EFGH-JKMN-PQRS-TVWX");
        when(trackingTokenService.hash("ABCD-EFGH-JKMN-PQRS-TVWX")).thenReturn("token-hash");
        when(categoryValidationService.requireSelectable("GENDER_BASED_VIOLENCE"))
                .thenReturn("GENDER_BASED_VIOLENCE");
        when(caseNumberGeneratorService.generate()).thenReturn("SC-2609-DEF345");
        when(reportRepository.save(any(IncidentReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().execute(new CreateIncidentReportInput(request, null));

        verify(lockService, never()).lock(any());
        verify(reportRepository, never()).findBySubmissionId(any());
        ArgumentCaptor<IncidentReport> reportCaptor = ArgumentCaptor.forClass(IncidentReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        assertNull(reportCaptor.getValue().getSubmissionId());
        assertNull(reportCaptor.getValue().getSubmissionFingerprint());
    }

    private CreateIncidentReportService service() {
        return new CreateIncidentReportService(
                reportRepository,
                historyRepository,
                caseNumberGeneratorService,
                trackingTokenService,
                accessService,
                responseFactory,
                categoryValidationService,
                fingerprintService,
                lockService
        );
    }

    private CreateIncidentReportRequest request(UUID submissionId, String description) {
        CreateIncidentReportRequest request = new CreateIncidentReportRequest();
        request.setSubmissionId(submissionId);
        request.setAnonymityMode(AnonymityMode.FULLY_ANONYMOUS);
        request.setIncidentType("GENDER_BASED_VIOLENCE");
        request.setUrgency(IncidentUrgency.HIGH);
        request.setDescription(description);
        request.setMatchingRequested(true);
        return request;
    }

    private IncidentReport existingGuest(UUID submissionId, String fingerprint, String tokenHash) {
        IncidentReport report = new IncidentReport();
        report.setSubmissionId(submissionId);
        report.setSubmissionFingerprint(fingerprint);
        report.setTrackingTokenHash(tokenHash);
        return report;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
