package co.tz.sheriaconnectapi.services.AuthServices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

@Service
public class TotpService {
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private final SecureRandom secureRandom = new SecureRandom();
    private final String issuer;

    public TotpService(@Value("${app.auth.mfa.issuer}") String issuer) {
        this.issuer = issuer;
    }

    public String generateSecret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public String provisioningUri(String email, String secret) {
        String label = encode(issuer + ":" + email);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    public boolean verify(String secret, String code) {
        return verifiedCounter(secret, code).isPresent();
    }

    public OptionalLong verifiedCounter(String secret, String code) {
        return verifiedCounter(secret, code, Instant.now());
    }

    OptionalLong verifiedCounter(String secret, String code, Instant instant) {
        if (code == null || !code.matches("\\d{6}")) {
            return OptionalLong.empty();
        }
        long counter = instant.getEpochSecond() / 30;
        for (long offset = -1; offset <= 1; offset++) {
            long candidate = counter + offset;
            if (generate(secret, candidate).equals(code)) {
                return OptionalLong.of(candidate);
            }
        }
        return OptionalLong.empty();
    }

    public List<String> recoveryCodes() {
        List<String> codes = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            byte[] bytes = new byte[8];
            secureRandom.nextBytes(bytes);
            StringBuilder value = new StringBuilder();
            for (byte item : bytes) {
                value.append(String.format(Locale.ROOT, "%02X", item));
            }
            codes.add(value.substring(0, 8) + "-" + value.substring(8, 16));
        }
        return codes;
    }

    private String generate(String secret, long counter) {
        try {
            byte[] key = decodeBase32(secret);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate TOTP", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32[(buffer >> (bitsLeft - 5)) & 31]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32[(buffer << (5 - bitsLeft)) & 31]);
        }
        return result.toString();
    }

    private byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "").toUpperCase(Locale.ROOT);
        ByteBuffer output = ByteBuffer.allocate(normalized.length() * 5 / 8 + 1);
        int buffer = 0;
        int bitsLeft = 0;
        for (char character : normalized.toCharArray()) {
            int decoded = character >= 'A' && character <= 'Z'
                    ? character - 'A'
                    : character - '2' + 26;
            if (decoded < 0 || decoded > 31) {
                continue;
            }
            buffer = (buffer << 5) | decoded;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.put((byte) ((buffer >> (bitsLeft - 8)) & 0xff));
                bitsLeft -= 8;
            }
        }
        byte[] result = new byte[output.position()];
        output.flip();
        output.get(result);
        return result;
    }
}
