package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.model.DTOs.IncidentReportResponse;
import co.tz.sheriaconnectapi.model.Entities.AdminCaseNote;
import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.IncidentReportReply;
import co.tz.sheriaconnectapi.repositories.AdminCaseNoteRepository;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.EvidenceFileRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportReplyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentReportResponseFactory {

    private final EvidenceFileRepository evidenceFileRepository;
    private final CaseStatusHistoryRepository caseStatusHistoryRepository;
    private final AdminCaseNoteRepository adminCaseNoteRepository;
    private final IncidentReportReplyRepository incidentReportReplyRepository;

    public IncidentReportResponseFactory(
            EvidenceFileRepository evidenceFileRepository,
            CaseStatusHistoryRepository caseStatusHistoryRepository,
            AdminCaseNoteRepository adminCaseNoteRepository,
            IncidentReportReplyRepository incidentReportReplyRepository
    ) {
        this.evidenceFileRepository = evidenceFileRepository;
        this.caseStatusHistoryRepository = caseStatusHistoryRepository;
        this.adminCaseNoteRepository = adminCaseNoteRepository;
        this.incidentReportReplyRepository = incidentReportReplyRepository;
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
        List<IncidentReportReply> reporterReplies =
                incidentReportReplyRepository.findByIncidentReportOrderByCreatedAtAsc(report);

        return new IncidentReportResponse(
                report,
                trackingToken,
                evidenceFiles,
                statusHistory,
                reporterReplies,
                adminNotes,
                includeAdminNotes
        );
    }
}
