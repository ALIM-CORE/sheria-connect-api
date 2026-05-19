package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.EvidenceFileResponse;
import co.tz.sheriaconnectapi.model.DTOs.UploadEvidenceInput;
import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.repositories.EvidenceFileRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UploadEvidenceService implements Command<UploadEvidenceInput, EvidenceFileResponse> {

    private final IncidentReportRepository incidentReportRepository;
    private final EvidenceFileRepository evidenceFileRepository;
    private final IncidentReportAccessService accessService;
    private final EvidenceStorageService evidenceStorageService;

    public UploadEvidenceService(
            IncidentReportRepository incidentReportRepository,
            EvidenceFileRepository evidenceFileRepository,
            IncidentReportAccessService accessService,
            EvidenceStorageService evidenceStorageService
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.evidenceFileRepository = evidenceFileRepository;
        this.accessService = accessService;
        this.evidenceStorageService = evidenceStorageService;
    }

    @Override
    public ResponseEntity<StandardResponse<EvidenceFileResponse>> execute(
            UploadEvidenceInput input
    ) {
        IncidentReport report = incidentReportRepository
                .findByCaseNumber(input.caseNumber())
                .orElseThrow(IncidentReportNotFoundException::new);

        accessService.assertCitizenAccess(
                report,
                input.authentication(),
                input.trackingToken()
        );

        EvidenceFile evidenceFile = evidenceStorageService.store(
                report,
                input.file(),
                input.uploadSource()
        );
        evidenceFile = evidenceFileRepository.save(evidenceFile);

        return ResponseUtil.success(
                new EvidenceFileResponse(evidenceFile),
                "Evidence uploaded successfully",
                HttpStatus.CREATED
        );
    }
}
