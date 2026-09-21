package com.ecommerce.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT Authentication Filter — runs on every request before routing.
 *
 * Responsibilities:
 * 1. Skip public paths (auth endpoints, actuator)
 * 2. Extract and validate the Bearer token
 * 3. Inject X-User-Id and X-User-Roles headers so downstream services
 *    don't need to re-validate the JWT — they trust the gateway
 * 4. Reject with 401 if token is missing or invalid
 *
 * This is the "API Gateway as security boundary" pattern:
 * downstream services can be on an internal network with no auth.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    // Paths that don't require a JWT
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/products",       // browsing products is public
            "/actuator"
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip auth for public paths
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = extractClaims(token);
            String userId = claims.getSubject();
            String roles  = claims.get("roles", String.class);

            // Mutate the request to add downstream headers
            // Downstream services read X-User-Id instead of re-parsing the JWT
            request.setAttribute("X-User-Id", userId);
            request.setAttribute("X-User-Roles", roles);

            // Add as actual headers via a wrapper so downstream RestTemplate/Feign sees them
            HeaderMutatingRequest mutated = new HeaderMutatingRequest(request);
            mutated.addHeader("X-User-Id", userId);
            if (roles != null) mutated.addHeader("X-User-Roles", roles);

            log.debug("JWT valid for userId={}, path={}", userId, path);
            chain.doFilter(mutated, response);

        } catch (Exception e) {
            log.warn("JWT validation failed for path={}: {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
        }
    }

    private Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}
