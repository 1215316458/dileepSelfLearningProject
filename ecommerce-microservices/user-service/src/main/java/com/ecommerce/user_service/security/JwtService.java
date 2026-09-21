package com.ecommerce.user_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

// JwtService — all JWT operations in one place
// JWT structure: header.payload.signature
//   header:    algorithm + token type
//   payload:   claims (sub, roles, iat, exp, type)
//   signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long      accessTokenExpiration;
    private final long      refreshTokenExpiration;

    public JwtService(
        @Value("${jwt.secret}")                    String secret,
        @Value("${jwt.access-token-expiration}")   long   accessTokenExpiration,
        @Value("${jwt.refresh-token-expiration}")  long   refreshTokenExpiration
    ) {
        // Keys.hmacShaKeyFor requires at least 256 bits for HS256
        this.signingKey             = Keys.hmacShaKeyFor(hexToBytes(secret));
        this.accessTokenExpiration  = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // --- Token generation ---

    public String generateAccessToken(UserDetails user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UserDetails user) {
        // refresh token carries minimal claims — just subject + type
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(signingKey)
                .compact();
    }

    // --- Token validation ---

    public boolean isTokenValid(String token, UserDetails user) {
        String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isTokenExpired(token);
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaim(token, c -> c.get("type", String.class)));
    }

    // --- Claims extraction ---

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    // --- Accessors for config values (used in tests + response) ---

    public long getAccessTokenExpiration() { return accessTokenExpiration; }

    // --- Private helpers ---

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        // parseSignedClaims verifies the signature — throws if tampered or expired
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
