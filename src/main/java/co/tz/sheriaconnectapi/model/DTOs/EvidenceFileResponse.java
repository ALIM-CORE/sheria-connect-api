package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Enums.EvidenceUploadSource;
import lombok.Getter;

import java.time.Instant;

@Getter
public class EvidenceFileResponse {
    private final Long id;
    private final String originalFileName;
    private final String contentType;
    private final long fileSize;
    private final String checksumSha256;
    private final EvidenceUploadSource uploadSource;
    private final Instant createdAt;

    public EvidenceFileResponse(EvidenceFile evidenceFile) {
        this.id = evidenceFile.getId();
        this.originalFileName = evidenceFile.getOriginalFileName();
        this.contentType = evidenceFile.getContentType();
        this.fileSize = evidenceFile.getFileSize();
        this.checksumSha256 = evidenceFile.getChecksumSha256();
        this.uploadSource = evidenceFile.getUploadSource();
        this.createdAt = evidenceFile.getCreatedAt();
    }
}
