package br.ufpb.iago.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ProxyManager<String> proxyManager;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled = true;

    @Value("${rate-limit.capacity:100}")
    private long capacity;

    @Value("${rate-limit.refill-tokens:100}")
    private long refillTokens;

    @Value("${rate-limit.refill-duration-minutes:1}")
    private long refillDurationMinutes;

    public RateLimitFilter(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/error") || path.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        String key = "rate_limit:" + ip;

        Supplier<BucketConfiguration> configSupplier = getConfigSupplier();

        ConsumptionProbe probe = null;
        try {
            probe = proxyManager.builder().build(key, configSupplier).tryConsumeAndReturnRemaining(1);
        } catch (Exception ex) {
            // ponytail: fail-open when Redis is unavailable so auxiliary rate-limiting never takes the API down
            log.warn("Redis indisponível no RateLimitFilter (fail-open): {}", ex.getMessage());
        }

        if (probe == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\": 429, \"message\": \"Muitas requisições. Tente novamente em " + waitForRefill
                            + " segundos.\", \"retry_after_seconds\": " + waitForRefill + "}"
            );
        }
    }

    private Supplier<BucketConfiguration> getConfigSupplier() {
        return () -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(capacity)
                    .refillGreedy(refillTokens, Duration.ofMinutes(refillDurationMinutes))
                    .build();
            return BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
        };
    }
}
