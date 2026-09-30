package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.model.DTOs.CreateIncidentReportRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.StringJoiner;

@Service
public class IncidentReportSubmissionFingerprintService {

    public String fingerprint(CreateIncidentReportRequest request) {
        StringJoiner canonical = new StringJoiner("|");
        add(canonical, enumValue(request.getAnonymityMode()));
        add(canonical, upperTrimmed(request.getIncidentType()));
        add(canonical, enumValue(request.getUrgency()));
        add(canonical, trimmed(request.getTitle()));
        add(canonical, trimmed(request.getDescription()));
        add(canonical, request.getIncidentDate() == null ? null : request.getIncidentDate().toString());
        add(canonical, trimmed(request.getLocationDescription()));
        add(canonical, trimmed(request.getRegion()));
        add(canonical, trimmed(request.getDistrict()));
        add(canonical, trimmed(request.getWard()));
        add(canonical, decimal(request.getLatitude()));
        add(canonical, decimal(request.getLongitude()));
        add(canonical, trimmed(request.getPseudonym()));
        add(canonical, trimmed(request.getContactName()));
        add(canonical, trimmed(request.getContactEmail()));
        add(canonical, trimmed(request.getContactPhone()));
        add(canonical, Boolean.TRUE.equals(request.getMatchingRequested()) ? "true" : "false");
        return sha256(canonical.toString());
    }

    private void add(StringJoiner joiner, String value) {
        String safe = value == null ? "" : value;
        joiner.add(safe.length() + ":" + safe);
    }

    private String enumValue(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    private String upperTrimmed(String value) {
        String valueTrimmed = trimmed(value);
        return valueTrimmed == null ? null : valueTrimmed.toUpperCase(Locale.ROOT);
    }

    private String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
