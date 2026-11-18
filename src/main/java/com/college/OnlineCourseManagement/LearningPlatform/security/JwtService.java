package com.college.OnlineCourseManagement.LearningPlatform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

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

    public String generateRefreshToken(UserDetails userDetails, boolean rememberMe) {
        return buildToken(userDetails,
                rememberMe ? rememberMeRefreshTokenExpiration : refreshTokenExpiration,
                REFRESH_TOKEN_TYPE);
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

    public Date extractExpirationDate(String token) {
        return extractExpiration(token);
    }

    public boolean isRememberMeRefreshToken(String token) {
        return extractExpiration(token).getTime() - extractClaim(token, Claims::getIssuedAt).getTime()
                > refreshTokenExpiration;
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
     * Requires a base64-encoded key with at least 256 bits of decoded entropy.
     */
    private byte[] toKeyBytes(String secret) {
    if (secret == null || secret.isBlank()) {
        throw new IllegalStateException("jwt.secret must be configured with a random secret of at least 32 bytes");
    }

    try {
        byte[] decoded = Decoders.BASE64.decode(secret.trim());
        if (decoded.length >= 32) {
            return decoded;
        }
        throw new IllegalStateException("jwt.secret must decode to at least 32 bytes");
    } catch (IllegalArgumentException | DecodingException ignored) {
        throw new IllegalStateException("jwt.secret must be valid base64 and decode to at least 32 bytes");
    }
}
}
