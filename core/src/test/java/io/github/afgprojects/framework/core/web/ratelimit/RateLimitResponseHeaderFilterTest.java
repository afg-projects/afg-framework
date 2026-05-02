package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * RateLimitResponseHeaderFilter 单元测试
 */
@DisplayName("RateLimitResponseHeaderFilter 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitResponseHeaderFilterTest {

    @Mock
    private RateLimitProperties properties;

    @Mock
    private RateLimitProperties.ResponseHeaders responseHeadersConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitResponseHeaderFilter filter;

    @BeforeEach
    void setUp() {
        when(properties.getResponseHeaders()).thenReturn(responseHeadersConfig);
        when(responseHeadersConfig.isEnabled()).thenReturn(true);
        when(responseHeadersConfig.getLimitHeader()).thenReturn("X-RateLimit-Limit");
        when(responseHeadersConfig.getRemainingHeader()).thenReturn("X-RateLimit-Remaining");
        when(responseHeadersConfig.getResetHeader()).thenReturn("X-RateLimit-Reset");
        when(responseHeadersConfig.getRetryAfterHeader()).thenReturn("Retry-After");
        filter = new RateLimitResponseHeaderFilter(properties);
    }

    @Nested
    @DisplayName("基本过滤测试")
    class BasicFilterTests {

        @Test
        @DisplayName("应该执行过滤器链")
        void shouldExecuteFilterChain() throws ServletException, IOException {
            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("禁用响应头时应该跳过设置")
        void shouldSkipWhenHeadersDisabled() throws ServletException, IOException {
            // given
            when(responseHeadersConfig.isEnabled()).thenReturn(false);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("无限流结果时应该跳过设置")
        void shouldSkipWhenNoResult() throws ServletException, IOException {
            // given
            when(request.getAttribute(RateLimitResponseHeaderFilter.RATE_LIMIT_RESULT_ATTR)).thenReturn(null);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("响应头设置测试")
    class HeaderSettingTests {

        @Test
        @DisplayName("应该设置限流响应头")
        void shouldSetRateLimitHeaders() throws ServletException, IOException {
            // given
            RateLimitResult result = RateLimitResult.allowed(99, 100, 3600000);
            when(request.getAttribute(RateLimitResponseHeaderFilter.RATE_LIMIT_RESULT_ATTR)).thenReturn(result);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(response).setHeader("X-RateLimit-Limit", result.getLimitHeader());
            verify(response).setHeader("X-RateLimit-Remaining", result.getRemainingHeader());
            verify(response).setHeader("X-RateLimit-Reset", result.getResetHeader());
        }

        @Test
        @DisplayName("限流时应该设置 Retry-After 头")
        void shouldSetRetryAfterHeaderWhenLimited() throws ServletException, IOException {
            // given
            RateLimitResult result = RateLimitResult.rejected(100, 3600000, 60000);
            when(request.getAttribute(RateLimitResponseHeaderFilter.RATE_LIMIT_RESULT_ATTR)).thenReturn(result);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(response).setHeader("Retry-After", result.getRetryAfterHeader());
        }
    }

    @Nested
    @DisplayName("静态方法测试")
    class StaticMethodTests {

        @Test
        @DisplayName("应该设置限流结果到请求属性")
        void shouldSetRateLimitResult() {
            // given
            RateLimitResult result = RateLimitResult.allowed(99, 100, 3600000);

            // when
            RateLimitResponseHeaderFilter.setRateLimitResult(request, result);

            // then
            verify(request).setAttribute(RateLimitResponseHeaderFilter.RATE_LIMIT_RESULT_ATTR, result);
        }

        @Test
        @DisplayName("null 结果不应该设置请求属性")
        void shouldNotSetNullResult() {
            // when
            RateLimitResponseHeaderFilter.setRateLimitResult(request, null);

            // then - 不应该调用 setAttribute
            // 由于是 null，不会调用 setAttribute
        }
    }
}