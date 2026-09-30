package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentReportStatusInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateIncidentReportStatusRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.services.NotificationServices.NotificationDispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateIncidentReportStatusServiceTest {

    @Mock
    private IncidentReportRepository reportRepository;
    @Mock
    private CaseStatusHistoryRepository historyRepository;
    @Mock
    private IncidentReportAccessService accessService;
    @Mock
    private IncidentReportResponseFactory responseFactory;
    @Mock
    private NotificationDispatchService notificationDispatchService;
    @Mock
    private Authentication authentication;

    @Test
    void needsInfoStatusRequiresAQuestion() {
        IncidentReport report = new IncidentReport();
        report.setCaseNumber("SC-2609-ABC234");
        report.setStatus(IncidentReportStatus.SUBMITTED);
        when(reportRepository.findByCaseNumber(report.getCaseNumber()))
                .thenReturn(Optional.of(report));

        UpdateIncidentReportStatusRequest request = new UpdateIncidentReportStatusRequest();
        request.setStatus(IncidentReportStatus.NEEDS_INFO);
        request.setNote("   ");

        assertThrows(
                UserNotValidException.class,
                () -> service().execute(new UpdateIncidentReportStatusInput(
                        report.getCaseNumber(),
                        request,
                        authentication
                ))
        );
    }

    private UpdateIncidentReportStatusService service() {
        return new UpdateIncidentReportStatusService(
                reportRepository,
                historyRepository,
                accessService,
                responseFactory,
                notificationDispatchService
        );
    }
}
