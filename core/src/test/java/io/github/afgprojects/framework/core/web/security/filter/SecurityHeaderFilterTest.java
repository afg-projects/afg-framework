package io.github.afgprojects.framework.core.web.security.filter;

import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SecurityHeaderFilter 单元测试。
 * <p>
 * 测试安全响应头过滤器的功能，验证 X-Content-Type-Options、X-Frame-Options、
 * X-XSS-Protection 和 Content-Security-Policy 头的设置。
 *
 * @see SecurityHeaderFilter
 */
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
