package com.ecommerce.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter — runs before JwtAuthFilter (Order = 1).
 *
 * Generates a UUID for every request and:
 * 1. Adds it to MDC so every log line for this request includes the ID
 * 2. Propagates it downstream via X-Correlation-Id header
 * 3. Echoes it back in the response so clients can correlate logs
 *
 * If the client already sent X-Correlation-Id (e.g. from a mobile app),
 * we reuse it — this allows end-to-end tracing across client + gateway + services.
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Put in MDC — all log statements in this thread will include correlationId
        MDC.put("correlationId", correlationId);

        // Echo back in response so the caller can correlate
        response.setHeader(CORRELATION_HEADER, correlationId);

        // Inject into downstream request
        HeaderMutatingRequest mutated = new HeaderMutatingRequest(request);
        mutated.addHeader(CORRELATION_HEADER, correlationId);

        try {
            chain.doFilter(mutated, response);
        } finally {
            // Always clean up MDC to prevent memory leaks in thread pools
            MDC.remove("correlationId");
        }
    }
}
