package co.tz.sheriaconnectapi.security.Jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Entities.User;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {

    private static final SecretKey SIGNING_KEY =
            Keys.hmacShaKeyFor(
                    signingSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

    // WEB
    private static final long WEB_ACCESS_EXPIRY = 5 * 60 * 1000;      // 5 min
    private static final long WEB_REFRESH_EXPIRY = 7L * 24 * 60 * 60 * 1000; // 7 days

    // MOBILE
    private static final long MOBILE_ACCESS_EXPIRY = 30 * 60 * 1000;  // 30 min
    private static final long MOBILE_REFRESH_EXPIRY = 30L * 24 * 60 * 60 * 1000; // 30 days

    public static String generateAccessToken(User user, AuthSession session) {
        long expiry = session.getClientType() == ClientType.MOBILE
                ? MOBILE_ACCESS_EXPIRY
                : WEB_ACCESS_EXPIRY;
        return generateSessionToken(user, session, expiry);
    }

    public static String generateRefreshToken(User user, AuthSession session) {
        long expiry = session.getClientType() == ClientType.MOBILE
                ? MOBILE_REFRESH_EXPIRY
                : WEB_REFRESH_EXPIRY;
        return generateSessionToken(user, session, expiry);
    }

    private static String generateSessionToken(User user, AuthSession session, long expiry) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getId().toString())
                .claim("sid", session.getSessionId())
                .audience()
                .add(session.getAudience())
                .and()
                .claim("active_context", session.getActiveContext().name())
                .claim("mfa_verified", session.isMfaVerified())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(SIGNING_KEY)
                .compact();
    }

    public static Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static boolean isTokenValid(String token) {
        try {
            return getClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private static String signingSecret() {
        String secret = System.getProperty("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = System.getenv("JWT_SECRET");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        return secret;
    }
}
