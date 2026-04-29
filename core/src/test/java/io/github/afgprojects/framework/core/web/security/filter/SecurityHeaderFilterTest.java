package io.github.afgprojects.framework.core.web.security.filter;

import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityHeaderFilterTest {

    private SecurityHeaderFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeaderFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void should_setSecurityHeaders_when_filterApplied() throws Exception {
        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "DENY");
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
    }

    @Test
    void should_continueFilterChain_when_headersSet() throws Exception {
        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void should_setCustomCsp_when_configured() throws Exception {
        filter.setContentSecurityPolicy("default-src 'self'");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("Content-Security-Policy", "default-src 'self'");
    }
}
