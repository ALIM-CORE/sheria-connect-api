package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.EvidenceFileTooLargeException;
import co.tz.sheriaconnectapi.exceptions.EvidenceStorageException;
import co.tz.sheriaconnectapi.exceptions.MissingEvidenceFileException;
import co.tz.sheriaconnectapi.exceptions.UnsupportedEvidenceTypeException;
import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.EvidenceUploadSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvidenceStorageService {

    private final Path storageRoot;
    private final long maxFileSizeBytes;
    private final Set<String> allowedContentTypes;

    public EvidenceStorageService(
            @Value("${app.evidence.storage-root:uploads/evidence}") String storageRoot,
            @Value("${app.evidence.max-file-size-bytes:10485760}") long maxFileSizeBytes,
            @Value("${app.evidence.allowed-content-types:image/jpeg,image/png,image/webp,video/mp4,audio/mpeg,audio/mp4,audio/wav,application/pdf,text/plain,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document}") String allowedContentTypes
    ) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.allowedContentTypes = Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    public EvidenceFile store(
            IncidentReport report,
            MultipartFile file,
            EvidenceUploadSource uploadSource
    ) {
        if (file == null || file.isEmpty()) {
            throw new MissingEvidenceFileException();
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new EvidenceFileTooLargeException();
        }

        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!allowedContentTypes.contains(contentType)) {
            throw new UnsupportedEvidenceTypeException();
        }

        try {
            byte[] bytes = file.getBytes();
            String checksum = sha256(bytes);
            String originalFileName = sanitizeOriginalName(file.getOriginalFilename());
            String extension = extensionOf(originalFileName);
            String storedFileName = UUID.randomUUID() + extension;
            String monthFolder = YearMonth.now().toString();
            Path relativePath = Path.of(monthFolder, report.getCaseNumber(), storedFileName);
            Path absolutePath = storageRoot.resolve(relativePath).normalize();

            if (!absolutePath.startsWith(storageRoot)) {
                throw new EvidenceStorageException();
            }

            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, bytes);

            EvidenceFile evidenceFile = new EvidenceFile();
            evidenceFile.setIncidentReport(report);
            evidenceFile.setOriginalFileName(originalFileName);
            evidenceFile.setStoredFileName(storedFileName);
            evidenceFile.setRelativePath(relativePath.toString().replace("\\", "/"));
            evidenceFile.setContentType(contentType);
            evidenceFile.setFileSize(file.getSize());
            evidenceFile.setChecksumSha256(checksum);
            evidenceFile.setUploadSource(uploadSource == null
                    ? EvidenceUploadSource.CITIZEN_MOBILE
                    : uploadSource);

            return evidenceFile;
        } catch (IOException ex) {
            throw new EvidenceStorageException();
        }
    }

    private String sanitizeOriginalName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "evidence-file";
        }

        return Path.of(originalFileName).getFileName().toString()
                .replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    private String extensionOf(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFileName.length() - 1) {
            return "";
        }
        return originalFileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
