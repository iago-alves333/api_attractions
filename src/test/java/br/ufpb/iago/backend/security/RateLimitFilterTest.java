package br.ufpb.iago.backend.security;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private RemoteBucketBuilder<String> bucketBuilder;

    @Mock
    private BucketProxy bucketProxy;

    @Mock
    private ConsumptionProbe consumptionProbe;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitFilter, "capacity", 100L);
        ReflectionTestUtils.setField(rateLimitFilter, "refillTokens", 100L);
        ReflectionTestUtils.setField(rateLimitFilter, "refillDurationMinutes", 1L);
    }

    @Test
    void doFilterInternal_shouldBypass_whenSwaggerOrDocs() throws Exception {
        when(request.getRequestURI()).thenReturn("/v3/api-docs");
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(proxyManager);
    }

    @Test
    void doFilterInternal_shouldAllowRequest_whenTokensAvailable() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/attractions");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(any(String.class), any(Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        
        when(consumptionProbe.isConsumed()).thenReturn(true);
        when(consumptionProbe.getRemainingTokens()).thenReturn(99L);
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(response).addHeader("X-Rate-Limit-Remaining", "99");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldBlockRequest_whenTokensExhausted() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/attractions");
        when(request.getRemoteAddr()).thenReturn("192.168.1.2"); // fallback IP test
        
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(any(String.class), any(Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        
        when(consumptionProbe.isConsumed()).thenReturn(false);
        when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(5_000_000_000L); // 5 seconds
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(response).setStatus(429);
        verify(response).addHeader("X-Rate-Limit-Retry-After-Seconds", "5");
        verify(response).setContentType("application/json");
        verifyNoInteractions(filterChain);
    }
}
