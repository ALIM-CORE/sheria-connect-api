package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.CaseInformationReplyNotAllowedException;
import co.tz.sheriaconnectapi.exceptions.DuplicateCaseInformationReplyException;
import co.tz.sheriaconnectapi.exceptions.InvalidCaseInformationReplyException;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportReplyInput;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportReplyRequest;
import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.IncidentReportReply;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportReplyRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyToIncidentReportServiceTest {

    @Mock
    private IncidentReportRepository reportRepository;
    @Mock
    private IncidentReportReplyRepository replyRepository;
    @Mock
    private CaseStatusHistoryRepository historyRepository;
    @Mock
    private IncidentReportAccessService accessService;
    @Mock
    private IncidentReportResponseFactory responseFactory;

    @Test
    void guestReplyIsStoredAndReturnsCaseToReview() {
        IncidentReport report = report(IncidentReportStatus.NEEDS_INFO);
        CaseStatusHistory requestHistory = needsInfoHistory(44L);
        when(reportRepository.findByCaseNumberForUpdate(report.getCaseNumber()))
                .thenReturn(Optional.of(report));
        when(historyRepository.findFirstByIncidentReportAndToStatusOrderByCreatedAtDesc(
                report,
                IncidentReportStatus.NEEDS_INFO
        )).thenReturn(Optional.of(requestHistory));
        when(replyRepository.existsByNeedsInfoHistory(requestHistory)).thenReturn(false);
        when(accessService.authenticatedUser(null)).thenReturn(Optional.empty());
        when(reportRepository.save(report)).thenReturn(report);

        service().execute(input("  It happened in Ilemela.  ", "guest-token"));

        verify(accessService).assertCitizenAccess(report, null, "guest-token");
        ArgumentCaptor<IncidentReportReply> replyCaptor =
                ArgumentCaptor.forClass(IncidentReportReply.class);
        verify(replyRepository).save(replyCaptor.capture());
        assertEquals("It happened in Ilemela.", replyCaptor.getValue().getBody());
        assertNull(replyCaptor.getValue().getSubmittedByUser());
        assertEquals(requestHistory, replyCaptor.getValue().getNeedsInfoHistory());
        assertEquals(IncidentReportStatus.UNDER_REVIEW, report.getStatus());

        ArgumentCaptor<CaseStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(CaseStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(IncidentReportStatus.NEEDS_INFO, historyCaptor.getValue().getFromStatus());
        assertEquals(IncidentReportStatus.UNDER_REVIEW, historyCaptor.getValue().getToStatus());
        assertNull(historyCaptor.getValue().getChangedByUser());
    }

    @Test
    void rejectsReplyWhenCaseIsNotWaitingForInformation() {
        IncidentReport report = report(IncidentReportStatus.UNDER_REVIEW);
        when(reportRepository.findByCaseNumberForUpdate(report.getCaseNumber()))
                .thenReturn(Optional.of(report));

        assertThrows(
                CaseInformationReplyNotAllowedException.class,
                () -> service().execute(input("Additional detail", "guest-token"))
        );
    }

    @Test
    void rejectsSecondReplyForSameRequest() {
        IncidentReport report = report(IncidentReportStatus.NEEDS_INFO);
        CaseStatusHistory requestHistory = needsInfoHistory(45L);
        when(reportRepository.findByCaseNumberForUpdate(report.getCaseNumber()))
                .thenReturn(Optional.of(report));
        when(historyRepository.findFirstByIncidentReportAndToStatusOrderByCreatedAtDesc(
                report,
                IncidentReportStatus.NEEDS_INFO
        )).thenReturn(Optional.of(requestHistory));
        when(replyRepository.existsByNeedsInfoHistory(requestHistory)).thenReturn(true);

        assertThrows(
                DuplicateCaseInformationReplyException.class,
                () -> service().execute(input("Second answer", "guest-token"))
        );
    }

    @Test
    void rejectsBlankAndOversizedRepliesBeforeDatabaseAccess() {
        assertThrows(
                InvalidCaseInformationReplyException.class,
                () -> service().execute(input("   ", "guest-token"))
        );
        assertThrows(
                InvalidCaseInformationReplyException.class,
                () -> service().execute(input("a".repeat(4001), "guest-token"))
        );
    }

    private ReplyToIncidentReportService service() {
        return new ReplyToIncidentReportService(
                reportRepository,
                replyRepository,
                historyRepository,
                accessService,
                responseFactory
        );
    }

    private IncidentReportReplyInput input(String body, String token) {
        return new IncidentReportReplyInput(
                "SC-2609-ABC234",
                new IncidentReportReplyRequest(body),
                token,
                null
        );
    }

    private IncidentReport report(IncidentReportStatus status) {
        IncidentReport report = new IncidentReport();
        report.setCaseNumber("SC-2609-ABC234");
        report.setStatus(status);
        return report;
    }

    private CaseStatusHistory needsInfoHistory(Long id) {
        CaseStatusHistory history = new CaseStatusHistory();
        history.setId(id);
        history.setToStatus(IncidentReportStatus.NEEDS_INFO);
        return history;
    }
}
