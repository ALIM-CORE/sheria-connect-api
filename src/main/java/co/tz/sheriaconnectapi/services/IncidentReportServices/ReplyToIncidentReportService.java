package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.CaseInformationReplyNotAllowedException;
import co.tz.sheriaconnectapi.exceptions.DuplicateCaseInformationReplyException;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.exceptions.InvalidCaseInformationReplyException;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportReplyInput;
import co.tz.sheriaconnectapi.model.DTOs.IncidentReportResponse;
import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.IncidentReportReply;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportReplyRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplyToIncidentReportService
        implements Command<IncidentReportReplyInput, IncidentReportResponse> {

    private static final int MAX_REPLY_LENGTH = 4000;

    private final IncidentReportRepository incidentReportRepository;
    private final IncidentReportReplyRepository replyRepository;
    private final CaseStatusHistoryRepository historyRepository;
    private final IncidentReportAccessService accessService;
    private final IncidentReportResponseFactory responseFactory;

    public ReplyToIncidentReportService(
            IncidentReportRepository incidentReportRepository,
            IncidentReportReplyRepository replyRepository,
            CaseStatusHistoryRepository historyRepository,
            IncidentReportAccessService accessService,
            IncidentReportResponseFactory responseFactory
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.replyRepository = replyRepository;
        this.historyRepository = historyRepository;
        this.accessService = accessService;
        this.responseFactory = responseFactory;
    }

    @Override
    @Transactional
    public ResponseEntity<StandardResponse<IncidentReportResponse>> execute(
            IncidentReportReplyInput input
    ) {
        String body = validatedBody(input);
        IncidentReport report = incidentReportRepository
                .findByCaseNumberForUpdate(input.caseNumber())
                .orElseThrow(IncidentReportNotFoundException::new);

        accessService.assertCitizenAccess(
                report,
                input.authentication(),
                input.trackingToken()
        );

        if (report.getStatus() != IncidentReportStatus.NEEDS_INFO) {
            throw new CaseInformationReplyNotAllowedException();
        }

        CaseStatusHistory requestHistory = historyRepository
                .findFirstByIncidentReportAndToStatusOrderByCreatedAtDesc(
                        report,
                        IncidentReportStatus.NEEDS_INFO
                )
                .orElseThrow(CaseInformationReplyNotAllowedException::new);

        if (replyRepository.existsByNeedsInfoHistory(requestHistory)) {
            throw new DuplicateCaseInformationReplyException();
        }

        IncidentReportReply reply = new IncidentReportReply();
        reply.setIncidentReport(report);
        reply.setNeedsInfoHistory(requestHistory);
        reply.setSubmittedByUser(
                accessService.authenticatedUser(input.authentication()).orElse(null)
        );
        reply.setBody(body);
        replyRepository.save(reply);

        report.setStatus(IncidentReportStatus.UNDER_REVIEW);
        report = incidentReportRepository.save(report);

        CaseStatusHistory statusHistory = new CaseStatusHistory();
        statusHistory.setIncidentReport(report);
        statusHistory.setFromStatus(IncidentReportStatus.NEEDS_INFO);
        statusHistory.setToStatus(IncidentReportStatus.UNDER_REVIEW);
        statusHistory.setChangedByUser(null);
        statusHistory.setNote("Reporter provided the requested information.");
        historyRepository.save(statusHistory);

        return ResponseUtil.success(
                responseFactory.build(report, null, false),
                "Requested information submitted successfully",
                HttpStatus.CREATED
        );
    }

    private String validatedBody(IncidentReportReplyInput input) {
        if (input.request() == null
                || input.request().body() == null
                || input.request().body().isBlank()) {
            throw new InvalidCaseInformationReplyException();
        }
        String body = input.request().body().trim();
        if (body.length() > MAX_REPLY_LENGTH) {
            throw new InvalidCaseInformationReplyException();
        }
        return body;
    }
}
