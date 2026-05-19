package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.YearMonth;

@Service
public class CaseNumberGeneratorService {

    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int RANDOM_LENGTH = 6;
    private final SecureRandom secureRandom = new SecureRandom();
    private final IncidentReportRepository incidentReportRepository;

    public CaseNumberGeneratorService(IncidentReportRepository incidentReportRepository) {
        this.incidentReportRepository = incidentReportRepository;
    }

    public String generate() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "SC-%s-%s".formatted(
                    YearMonth.now().toString().replace("-", "").substring(2),
                    randomCode()
            );

            if (!incidentReportRepository.existsByCaseNumber(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate unique case number");
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(RANDOM_LENGTH);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            builder.append(CHARS[secureRandom.nextInt(CHARS.length)]);
        }
        return builder.toString();
    }
}
