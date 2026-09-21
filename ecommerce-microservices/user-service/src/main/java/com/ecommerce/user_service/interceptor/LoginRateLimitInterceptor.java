package com.ecommerce.user_service.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// HandlerInterceptor vs Filter:
//   Filter  — runs in the Servlet container, before Spring MVC dispatching
//   Interceptor — runs inside Spring MVC, after DispatcherServlet, has access to handler info
// Rate limiting login attempts per IP to prevent brute-force attacks
@Component
public class LoginRateLimitInterceptor implements HandlerInterceptor {

    // max login attempts per IP within the window
    private static final int    MAX_ATTEMPTS  = 5;
    private static final long   WINDOW_MS     = 60_000L;  // 1 minute

    // IP → [attempt count, window start timestamp]
    private final Map<String, long[]> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String ip = request.getRemoteAddr();
        long   now = System.currentTimeMillis();

        attempts.compute(ip, (key, val) -> {
            if (val == null || now - val[1] > WINDOW_MS) {
                // first attempt or window expired — reset
                return new long[]{1, now};
            }
            val[0]++;   // increment attempt count
            return val;
        });

        long[] state = attempts.get(ip);
        if (state[0] > MAX_ATTEMPTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Too many login attempts. Try again in 1 minute.\"}");
            return false;   // stop processing — don't call the controller
        }

        return true;    // proceed to controller
    }
}
