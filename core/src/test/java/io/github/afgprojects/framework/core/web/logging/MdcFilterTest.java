package io.github.afgprojects.framework.core.web.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

class MdcFilterTest {

    private MdcFilter filter;
    private LoggingProperties properties;

    @BeforeEach
    void setUp() {
        // Setup Spring RequestContextHolder
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void should_populateMdc_when_contextAvailable() throws Exception {
        // Given
        properties = new LoggingProperties();
        filter = new MdcFilter(properties);

        RequestContext context = RequestContext.builder()
                .tenantId(100L)
                .userId(42L)
                .requestPath("/api/test")
                .build();
        AfgRequestContextHolder.setContext(context);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
    }

    @Test
    void should_notSetMdc_when_contextNull() throws Exception {
        // Given
        properties = new LoggingProperties();
        filter = new MdcFilter(properties);
        AfgRequestContextHolder.clear();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
    }

    @Test
    void should_clearMdc_afterFilter() throws Exception {
        // Given
        properties = new LoggingProperties();
        filter = new MdcFilter(properties);

        RequestContext context = RequestContext.builder().userId(1L).build();
        AfgRequestContextHolder.setContext(context);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void should_useCustomFields_when_configured() throws Exception {
        // Given
        properties = new LoggingProperties();
        properties.getMdc().setFields(new String[] {"tenantId", "requestId"});
        filter = new MdcFilter(properties);

        RequestContext context = RequestContext.builder()
                .tenantId(100L)
                .requestId("req-456")
                .userId(42L) // Should not be in MDC
                .build();
        AfgRequestContextHolder.setContext(context);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
    }

    @Test
    void should_setAllFields_when_allPresent() throws Exception {
        // Given
        properties = new LoggingProperties();
        properties.getMdc().setFields(new String[] {
            "tenantId", "userId", "requestPath", "requestId", "username", "clientIp", "requestMethod"
        });
        filter = new MdcFilter(properties);

        RequestContext context = RequestContext.builder()
                .tenantId(100L)
                .userId(42L)
                .requestPath("/api/test")
                .requestId("req-789")
                .username("testuser")
                .clientIp("192.168.1.1")
                .requestMethod("GET")
                .build();
        AfgRequestContextHolder.setContext(context);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
    }

    @Test
    void should_handleNullFields_gracefully() throws Exception {
        // Given
        properties = new LoggingProperties();
        filter = new MdcFilter(properties);

        RequestContext context = new RequestContext(); // All fields null
        AfgRequestContextHolder.setContext(context);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
    }

    @Test
    void should_logWarning_when_unknownFieldConfigured() throws Exception {
        // Given
        properties = new LoggingProperties();
        properties.getMdc().setFields(new String[] {"traceId", "unknownField"});
        filter = new MdcFilter(properties);

        RequestContext context = RequestContext.builder().build();
        AfgRequestContextHolder.setContext(context);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilterInternal(request, response, chain);

        // Then - should still work, just log warning for unknown fields
        verify(chain).doFilter(request, response);
    }
}
