package com.college.OnlineCourseManagement.LearningPlatform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final long rememberMeRefreshTokenExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${jwt.remember-me-refresh-token-expiration}") long rememberMeRefreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(toKeyBytes(secret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.rememberMeRefreshTokenExpiration = rememberMeRefreshTokenExpiration;
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails, accessTokenExpiration, ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, refreshTokenExpiration, REFRESH_TOKEN_TYPE);
    }

    /**
     * "Remember me": issues a refresh token with the longer lifetime. The access token
     * lifetime is unchanged, so the client must still refresh periodically.
     */
    public String generateRememberMeRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, rememberMeRefreshTokenExpiration, REFRESH_TOKEN_TYPE);
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        // isEnabled() is honoured so disabling an account revokes its live tokens too.
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && userDetails.isEnabled();
    }

    private String buildToken(UserDetails userDetails, long expiration, String tokenType) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(TOKEN_TYPE_CLAIM, tokenType);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
 * Accepts either a base64-encoded secret or a raw passphrase. A base64 value that
 * decodes to fewer than 32 bytes (too weak for HS512) falls back to the raw bytes.
 *
 * When no secret is configured at all, a random ephemeral one is generated so local
 * development still works. It changes on every restart, which signs everyone out - hence
 * the loud warning.
 */
private byte[] toKeyBytes(String secret) {
    if (secret == null || secret.isBlank()) {
        log.warn("""
                jwt.secret is not configured (set the JWT_SECRET environment variable).
                Generating a RANDOM secret for this run only - all sessions will be
                invalidated when the application restarts. Never run production like this.""");
        byte[] random = new byte[64];
        new SecureRandom().nextBytes(random);
        return random;
    }

    try {
        byte[] decoded = Decoders.BASE64.decode(secret.trim());

        if (decoded.length >= 32) {
            return decoded;
        }
    } catch (IllegalArgumentException ignored) {
        // Not valid base64 - treat the value as a raw passphrase below.
    }

    byte[] raw = secret.getBytes(StandardCharsets.UTF_8);

    if (raw.length < 32) {
        // Pad rather than fail: HMAC-SHA512 needs >= 256 bits and a short secret would
        // otherwise abort startup with an obscure WeakKeyException.
        log.warn("jwt.secret is shorter than 32 bytes; padding it to a safe length. Use a longer secret.");
        byte[] padded = new byte[32];
        System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, padded.length));
        return padded;
    }

    return raw;
}
}
