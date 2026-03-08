package co.tz.sheriaconnectapi.security.Jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class JwtUtil {

    private static final SecretKey SIGNING_KEY =
            Keys.hmacShaKeyFor(
                    System.getenv("JWT_SECRET").getBytes()
            );

    // WEB
    private static final long WEB_ACCESS_EXPIRY = 5 * 60 * 1000;      // 5 min
    private static final long WEB_REFRESH_EXPIRY = 7L * 24 * 60 * 60 * 1000; // 7 days

    // MOBILE
    private static final long MOBILE_ACCESS_EXPIRY = 30 * 60 * 1000;  // 30 min
    private static final long MOBILE_REFRESH_EXPIRY = 30L * 24 * 60 * 60 * 1000; // 30 days

    public static String generateAccessToken(UserDetails user, ClientType clientType) {
        long expiry = clientType == ClientType.MOBILE
                ? MOBILE_ACCESS_EXPIRY
                : WEB_ACCESS_EXPIRY;

        return generateToken(user, expiry);
    }

    public static String generateRefreshToken(UserDetails user, ClientType clientType) {
        long expiry = clientType == ClientType.MOBILE
                ? MOBILE_REFRESH_EXPIRY
                : WEB_REFRESH_EXPIRY;

        return generateToken(user, expiry);
    }

    private static String generateToken(UserDetails user, long expiry) {
        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("authorities", authorities)
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

    public static String extractUsername(String token) {
        return getClaims(token).getSubject();
    }


}
