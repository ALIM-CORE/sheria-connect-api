package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.EvidenceUploadSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "evidence_files",
        indexes = {
                @Index(name = "idx_evidence_files_report_id", columnList = "incident_report_id"),
                @Index(name = "idx_evidence_files_checksum", columnList = "checksum_sha256")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class EvidenceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_report_id", nullable = false)
    private IncidentReport incidentReport;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "relative_path", nullable = false, length = 500)
    private String relativePath;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_source", nullable = false, length = 32)
    private EvidenceUploadSource uploadSource = EvidenceUploadSource.CITIZEN_MOBILE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
