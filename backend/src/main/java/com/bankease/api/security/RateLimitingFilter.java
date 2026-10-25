package com.bankease.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet filter that enforces per-IP rate limiting using the Bucket4j token-bucket algorithm.
 *
 * <p>Each unique client IP address is allocated its own in-memory {@link Bucket}. When a bucket
 * is exhausted the filter short-circuits the request and returns HTTP <strong>429 Too Many
 * Requests</strong> with a JSON error body, before any application logic executes.</p>
 *
 * <p>Configuration properties (application.yml):</p>
 * <pre>
 * app:
 *   rate-limit:
 *     capacity: 100          # max tokens in the bucket (burst limit)
 *     refill-tokens: 100     # tokens added per refill period
 *     refill-period-seconds: 60
 * </pre>
 *
 * <p><strong>Note:</strong> The in-memory {@link ConcurrentHashMap} works well for a single
 * instance deployment. For a clustered/distributed deployment, replace it with a Bucket4j
 * distributed backend (e.g., Redis via Bucket4j-Redis).</p>
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    /** Maximum number of tokens a bucket may hold (also the initial fill). */
    private final long capacity;

    /** Number of tokens added to the bucket after each refill period. */
    private final long refillTokens;

    /** Duration (in seconds) between bucket refills. */
    private final long refillPeriodSeconds;

    /** Per-IP bucket registry. */
    private final ConcurrentHashMap<String, Bucket> ipBucketCache = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    /**
     * Constructs the filter with rate-limit configuration injected from application properties.
     *
     * @param capacity             max token capacity per bucket
     * @param refillTokens         tokens restored per refill period
     * @param refillPeriodSeconds  refill period in seconds
     * @param objectMapper         Jackson mapper for serialising error responses
     */
    public RateLimitingFilter(
            @Value("${app.rate-limit.capacity:100}")               long capacity,
            @Value("${app.rate-limit.refill-tokens:100}")          long refillTokens,
            @Value("${app.rate-limit.refill-period-seconds:60}")   long refillPeriodSeconds,
            ObjectMapper objectMapper) {
        this.capacity            = capacity;
        this.refillTokens        = refillTokens;
        this.refillPeriodSeconds = refillPeriodSeconds;
        this.objectMapper        = objectMapper;
    }

    /**
     * Checks the rate-limit bucket for the requesting IP. If a token is available,
     * the request proceeds; otherwise a 429 response is returned immediately.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        Bucket  bucket  = ipBucketCache.computeIfAbsent(clientIp, this::createNewBucket);

        if (bucket.tryConsume(1)) {
            log.trace("Rate-limit check passed for IP '{}'. Remaining tokens: {}",
                    clientIp, bucket.getAvailableTokens());
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP '{}'. Returning HTTP 429.", clientIp);
            writeRateLimitExceededResponse(response, clientIp);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@link Bucket} with the configured capacity and greedy refill policy.
     *
     * @param ip the client IP (unused in bucket construction; present for {@code computeIfAbsent})
     * @return a fully initialised {@link Bucket}
     */
    private Bucket createNewBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.greedy(refillTokens, Duration.ofSeconds(refillPeriodSeconds)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Resolves the real client IP, honouring the {@code X-Forwarded-For} header set by
     * reverse proxies / load balancers.
     *
     * @param request the HTTP request
     * @return the client's IP address string
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // The header may contain a comma-separated list; the first entry is the real client.
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes a JSON {@code 429 Too Many Requests} error body to the response.
     *
     * @param response the HTTP response to populate
     * @param clientIp the IP address that triggered the limit (for logging context)
     * @throws IOException if writing to the response stream fails
     */
    private void writeRateLimitExceededResponse(HttpServletResponse response, String clientIp)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = Map.of(
                "timestamp", Instant.now().toString(),
                "status",    HttpStatus.TOO_MANY_REQUESTS.value(),
                "error",     "Too Many Requests",
                "message",   "Rate limit exceeded. Please slow down your requests and try again later.",
                "path",      "N/A"
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
