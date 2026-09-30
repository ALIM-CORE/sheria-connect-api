package co.tz.sheriaconnectapi.services.IncidentReportServices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class TrackingTokenService {

    private static final String CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int TOKEN_CHARACTERS = 20;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DERIVATION_DOMAIN = "sheria-connect-report-token-v1:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec derivationKey;

    public TrackingTokenService(
            @Value("${app.incident-report.tracking-token-derivation-key}") String rawKey
    ) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("Tracking-token derivation key is required");
        }
        this.derivationKey = new SecretKeySpec(
                rawKey.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
        );
    }

    public String generateToken() {
        StringBuilder compact = new StringBuilder(TOKEN_CHARACTERS);
        for (int index = 0; index < TOKEN_CHARACTERS; index++) {
            compact.append(CROCKFORD_ALPHABET.charAt(secureRandom.nextInt(32)));
        }
        return format(compact.toString());
    }

    public String deriveToken(UUID submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("Submission ID is required to derive a tracking token");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(derivationKey);
            byte[] digest = mac.doFinal(
                    (DERIVATION_DOMAIN + submissionId).getBytes(StandardCharsets.UTF_8)
            );
            return format(crockfordEncodeFirst100Bits(digest));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not derive tracking token", exception);
        }
    }

    public String hash(String token) {
        return sha256(normalize(token));
    }

    public boolean matches(String token, String expectedHash) {
        if (token == null || token.isBlank() || expectedHash == null || expectedHash.isBlank()) {
            return false;
        }

        if (constantTimeEquals(sha256(token), expectedHash)) {
            return true;
        }

        String normalized = normalize(token);
        return !normalized.equals(token)
                && constantTimeEquals(sha256(normalized), expectedHash);
    }

    String normalize(String token) {
        if (token == null) {
            return "";
        }

        return token
                .replace("-", "")
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT)
                .replace('O', '0')
                .replace('I', '1')
                .replace('L', '1');
    }

    private String crockfordEncodeFirst100Bits(byte[] bytes) {
        StringBuilder encoded = new StringBuilder(TOKEN_CHARACTERS);
        int bitIndex = 0;
        for (int character = 0; character < TOKEN_CHARACTERS; character++) {
            int value = 0;
            for (int bit = 0; bit < 5; bit++) {
                int absoluteBit = bitIndex++;
                int byteIndex = absoluteBit / 8;
                int bitInByte = 7 - (absoluteBit % 8);
                value = (value << 1) | ((bytes[byteIndex] >>> bitInByte) & 1);
            }
            encoded.append(CROCKFORD_ALPHABET.charAt(value));
        }
        return encoded.toString();
    }

    private String format(String compact) {
        StringBuilder formatted = new StringBuilder(24);
        for (int index = 0; index < compact.length(); index++) {
            if (index > 0 && index % 4 == 0) {
                formatted.append('-');
            }
            formatted.append(compact.charAt(index));
        }
        return formatted.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean constantTimeEquals(String actual, String expected) {
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }
}
