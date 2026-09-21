package com.ecommerce.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiter Filter — Token Bucket algorithm, in-memory per client IP.
 *
 * Token Bucket:
 * - Each client gets a bucket with capacity = MAX_TOKENS
 * - Tokens refill at REFILL_RATE per second
 * - Each request consumes 1 token
 * - If bucket is empty → 429 Too Many Requests
 *
 * In production this would use Redis so all gateway instances share state.
 * Here we use ConcurrentHashMap (single instance, dev/learning only).
 *
 * Runs at Order(2) — after CorrelationId, before JWT auth.
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int MAX_TOKENS    = 20;   // burst capacity per client
    private static final int REFILL_RATE   = 10;   // tokens added per second

    // clientIp → bucket
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket());

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for IP={}, path={}", clientIp, request.getRequestURI());
            response.setStatus(429);
            response.setHeader("Retry-After", "1");
            response.getWriter().write("{\"error\":\"Too many requests\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For is set by load balancers/proxies in front of the gateway
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Token Bucket — thread-safe, lock-free using AtomicInteger + volatile timestamp.
     */
    private static class TokenBucket {
        private final AtomicInteger tokens = new AtomicInteger(MAX_TOKENS);
        private volatile long lastRefillTime = Instant.now().getEpochSecond();

        boolean tryConsume() {
            refill();
            // CAS loop — atomically decrement only if > 0
            while (true) {
                int current = tokens.get();
                if (current <= 0) return false;
                if (tokens.compareAndSet(current, current - 1)) return true;
            }
        }

        private void refill() {
            long now = Instant.now().getEpochSecond();
            long elapsed = now - lastRefillTime;
            if (elapsed > 0) {
                int toAdd = (int) (elapsed * REFILL_RATE);
                if (toAdd > 0) {
                    tokens.updateAndGet(t -> Math.min(MAX_TOKENS, t + toAdd));
                    lastRefillTime = now;
                }
            }
        }
    }
}
