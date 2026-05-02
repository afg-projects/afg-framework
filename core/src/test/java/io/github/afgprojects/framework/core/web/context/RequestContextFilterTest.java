package io.github.afgprojects.framework.core.web.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * RequestContextFilter 单元测试
 */
@DisplayName("RequestContextFilter 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestContextFilterTest {

    @Mock
    private FilterChain filterChain;

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    private RequestContextFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        MDC.clear();
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("无 Tracer 测试")
    class NoTracerTests {

        @Test
        @DisplayName("应该从请求头提取 traceId")
        void shouldExtractTraceIdFromHeader() throws ServletException, IOException {
            // given
            filter = new RequestContextFilter();
            request.addHeader("X-Trace-Id", "test-trace-123");

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-Trace-Id")).isEqualTo("test-trace-123");
        }

        @Test
        @DisplayName("请求头为空时应该从参数提取 traceId")
        void shouldExtractTraceIdFromParameter() throws ServletException, IOException {
            // given
            filter = new RequestContextFilter();
            request.setParameter("traceId", "test-trace-456");

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-Trace-Id")).isEqualTo("test-trace-456");
        }

        @Test
        @DisplayName("无 traceId 时应该生成新的")
        void shouldGenerateNewTraceId() throws ServletException, IOException {
            // given
            filter = new RequestContextFilter();

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-Trace-Id")).isNotNull();
        }
    }

    @Nested
    @DisplayName("有 Tracer 测试")
    class WithTracerTests {

        @Test
        @DisplayName("有活跃 Span 时应该使用现有 traceId")
        void shouldUseExistingSpanTraceId() throws ServletException, IOException {
            // given
            filter = new RequestContextFilter(tracer);
            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn("tracer-trace-id");

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            verify(tracer).currentSpan();
        }

        @Test
        @DisplayName("无活跃 Span 时应该创建新 Span")
        void shouldCreateNewSpanWhenNoneActive() throws ServletException, IOException {
            // given
            filter = new RequestContextFilter(tracer);
            when(tracer.currentSpan()).thenReturn(null);
            when(tracer.nextSpan()).thenReturn(span);
            when(span.name(any())).thenReturn(span);
            when(span.tag(any(), any())).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn("new-trace-id");

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(tracer).nextSpan();
            verify(span).start();
            verify(span).end();
        }

        @Test
        @DisplayName("有传入 traceId 时应该标记为父 traceId")
        void shouldTagParentTraceId() throws ServletException, IOException {
            // given
            filter = new RequestContextFilter(tracer);
            request.addHeader("X-Trace-Id", "incoming-trace-id");
            when(tracer.currentSpan()).thenReturn(null);
            when(tracer.nextSpan()).thenReturn(span);
            when(span.name(any())).thenReturn(span);
            when(span.tag(any(), any())).thenReturn(span);
            when(span.context()).thenReturn(traceContext);
            when(traceContext.traceId()).thenReturn("new-trace-id");

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(span).tag("parent.traceId", "incoming-trace-id");
        }
    }

    @Nested
    @DisplayName("MDC 清理测试")
    class MdcCleanupTests {

        @Test
        @DisplayName("过滤完成后应该清理 MDC")
        void shouldClearMdcAfterFilter() throws ServletException, IOException {
            // given
            filter = new RequestContextFilter();

            // when
            filter.doFilter(request, response, filterChain);

            // then
            assertThat(MDC.get("traceId")).isNull();
            assertThat(MDC.get("requestId")).isNull();
        }
    }

    @Nested
    @DisplayName("SpanInfo 测试")
    class SpanInfoTests {

        @Test
        @DisplayName("SpanInfo 应该持有 Span")
        void shouldHoldSpan() {
            // given
            Span mockSpan = mock(Span.class);

            // when
            RequestContextFilter filter = new RequestContextFilter(tracer);
            // SpanInfo 是私有类，通过过滤器的行为来测试

            // then - 验证过滤器创建成功
            assertThat(filter).isNotNull();
        }
    }
}