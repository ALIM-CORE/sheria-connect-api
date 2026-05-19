package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.EvidenceUploadSource;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public record UploadEvidenceInput(
        String caseNumber,
        String trackingToken,
        MultipartFile file,
        EvidenceUploadSource uploadSource,
        Authentication authentication
) {
}
