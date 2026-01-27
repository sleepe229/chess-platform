package com.chess.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${rate-limit.login.max-requests:5}")
    private int loginMaxRequests;

    @Value("${rate-limit.login.window-seconds:60}")
    private int loginWindowSeconds;

    @Value("${rate-limit.register.max-requests:3}")
    private int registerMaxRequests;

    @Value("${rate-limit.register.window-seconds:300}")
    private int registerWindowSeconds;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        // Apply rate limiting only to login and register endpoints
        if (path.contains("/login")) {
            if (!checkRateLimit("login:" + clientIp, loginMaxRequests, loginWindowSeconds)) {
                log.warn("Rate limit exceeded for login from IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many login attempts. Please try again later.\"}");
                return;
            }
        } else if (path.contains("/register")) {
            if (!checkRateLimit("register:" + clientIp, registerMaxRequests, registerWindowSeconds)) {
                log.warn("Rate limit exceeded for register from IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many registration attempts. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean checkRateLimit(String key, int maxRequests, int windowSeconds) {
        try {
            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr == null ? 0 : Integer.parseInt(countStr);

            if (count >= maxRequests) {
                return false;
            }

            if (count == 0) {
                redisTemplate.opsForValue().set(key, "1", windowSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().increment(key);
            }

            return true;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // On error, allow the request to proceed (fail open)
            return true;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

