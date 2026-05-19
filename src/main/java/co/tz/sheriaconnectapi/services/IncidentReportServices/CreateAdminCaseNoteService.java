package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.AdminCaseNoteResponse;
import co.tz.sheriaconnectapi.model.DTOs.CreateAdminCaseNoteInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateAdminCaseNoteRequest;
import co.tz.sheriaconnectapi.model.Entities.AdminCaseNote;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.AdminCaseNoteRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CreateAdminCaseNoteService
        implements Command<CreateAdminCaseNoteInput, AdminCaseNoteResponse> {

    private final IncidentReportRepository incidentReportRepository;
    private final AdminCaseNoteRepository adminCaseNoteRepository;
    private final IncidentReportAccessService accessService;

    public CreateAdminCaseNoteService(
            IncidentReportRepository incidentReportRepository,
            AdminCaseNoteRepository adminCaseNoteRepository,
            IncidentReportAccessService accessService
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.adminCaseNoteRepository = adminCaseNoteRepository;
        this.accessService = accessService;
    }

    @Override
    public ResponseEntity<StandardResponse<AdminCaseNoteResponse>> execute(
            CreateAdminCaseNoteInput input
    ) {
        CreateAdminCaseNoteRequest request = input.request();
        if (request == null || request.getNote() == null || request.getNote().isBlank()) {
            throw new UserNotValidException("Admin note is required");
        }

        IncidentReport report = incidentReportRepository
                .findByCaseNumber(input.caseNumber())
                .orElseThrow(IncidentReportNotFoundException::new);
        User adminUser = accessService.authenticatedUser(input.authentication())
                .orElse(null);

        AdminCaseNote note = new AdminCaseNote();
        note.setIncidentReport(report);
        note.setAdminUser(adminUser);
        note.setNote(request.getNote().trim());
        note = adminCaseNoteRepository.save(note);

        return ResponseUtil.success(
                new AdminCaseNoteResponse(note),
                "Admin note added successfully",
                HttpStatus.CREATED
        );
    }
}
