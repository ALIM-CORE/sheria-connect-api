package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.model.DTOs.IncidentReportResponse;
import co.tz.sheriaconnectapi.model.Entities.AdminCaseNote;
import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.repositories.AdminCaseNoteRepository;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.EvidenceFileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentReportResponseFactory {

    private final EvidenceFileRepository evidenceFileRepository;
    private final CaseStatusHistoryRepository caseStatusHistoryRepository;
    private final AdminCaseNoteRepository adminCaseNoteRepository;

    public IncidentReportResponseFactory(
            EvidenceFileRepository evidenceFileRepository,
            CaseStatusHistoryRepository caseStatusHistoryRepository,
            AdminCaseNoteRepository adminCaseNoteRepository
    ) {
        this.evidenceFileRepository = evidenceFileRepository;
        this.caseStatusHistoryRepository = caseStatusHistoryRepository;
        this.adminCaseNoteRepository = adminCaseNoteRepository;
    }

    public IncidentReportResponse build(
            IncidentReport report,
            String trackingToken,
            boolean includeAdminNotes
    ) {
        List<EvidenceFile> evidenceFiles =
                evidenceFileRepository.findByIncidentReportOrderByCreatedAtDesc(report);
        List<CaseStatusHistory> statusHistory =
                caseStatusHistoryRepository.findByIncidentReportOrderByCreatedAtAsc(report);
        List<AdminCaseNote> adminNotes = includeAdminNotes
                ? adminCaseNoteRepository.findByIncidentReportOrderByCreatedAtDesc(report)
                : List.of();

        return new IncidentReportResponse(
                report,
                trackingToken,
                evidenceFiles,
                statusHistory,
                adminNotes
        );
    }
}
