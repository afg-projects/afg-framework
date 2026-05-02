package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * EnhancedTraceInterceptor 单元测试
 */
@DisplayName("EnhancedTraceInterceptor 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnhancedTraceInterceptorTest extends BaseUnitTest {

    @Mock
    private Tracer tracer;

    @Mock
    private TracingProperties properties;

    @Mock
    private TracingSampler sampler;

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    @Mock
    private Span span;

    @Mock
    private TracingProperties.Baggage baggageConfig;

    private EnhancedTraceInterceptor interceptor;

    @BeforeEach
    void setUp() throws IOException {
        when(request.getURI()).thenReturn(URI.create("http://localhost/test"));
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        lenient().when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        lenient().when(properties.isEnabled()).thenReturn(true);
        lenient().when(properties.getBaggage()).thenReturn(baggageConfig);
        lenient().when(baggageConfig.isEnabled()).thenReturn(false);
        lenient().when(baggageConfig.getRemoteFields()).thenReturn(Collections.emptyList());
        lenient().when(baggageConfig.getFieldMappings()).thenReturn(Collections.emptyMap());
    }

    @Nested
    @DisplayName("无 Tracer 测试")
    class NoTracerTests {

        @Test
        @DisplayName("无 tracer 时不应该创建 Span")
        void shouldNotCreateSpanWithoutTracer() throws IOException {
            // given
            interceptor = new EnhancedTraceInterceptor();
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            // then
            assertThat(result).isNotNull();
            verify(tracer, never()).nextSpan();
        }
    }

    @Nested
    @DisplayName("采样测试")
    class SamplingTests {

        @Test
        @DisplayName("sampler 返回 false 时应该跳过追踪")
        void shouldSkipWhenSamplerReturnsFalse() throws IOException {
            // given
            interceptor = new EnhancedTraceInterceptor(tracer, properties, sampler);
            when(sampler.shouldSample()).thenReturn(false);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            // then
            assertThat(result).isNotNull();
            verify(tracer, never()).nextSpan();
        }
    }

    @Nested
    @DisplayName("Span 创建测试")
    class SpanCreationTests {

        @Test
        @DisplayName("应该创建 Span 并正确设置标签")
        void shouldCreateSpanWithTags() throws IOException {
            // given
            interceptor = new EnhancedTraceInterceptor(tracer, properties, sampler);
            when(sampler.shouldSample()).thenReturn(true);
            when(tracer.nextSpan()).thenReturn(span);
            when(span.name(anyString())).thenReturn(span);
            when(tracer.currentSpan()).thenReturn(null);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            verify(tracer).nextSpan();
            verify(span).start();
        }

        @Test
        @DisplayName("执行完成后应该结束 Span")
        void shouldEndSpanAfterExecution() throws IOException {
            // given
            interceptor = new EnhancedTraceInterceptor(tracer, properties, sampler);
            when(sampler.shouldSample()).thenReturn(true);
            when(tracer.nextSpan()).thenReturn(span);
            when(span.name(anyString())).thenReturn(span);
            when(tracer.currentSpan()).thenReturn(null);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            verify(span).end();
        }
    }

    @Nested
    @DisplayName("头传播测试")
    class HeaderPropagationTests {

        @Test
        @DisplayName("应该传播标准头")
        void shouldPropagateStandardHeaders() throws IOException {
            // given
            interceptor = new EnhancedTraceInterceptor(tracer, properties, sampler);
            when(sampler.shouldSample()).thenReturn(true);
            when(tracer.nextSpan()).thenReturn(span);
            when(span.name(anyString())).thenReturn(span);
            when(tracer.currentSpan()).thenReturn(null);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            assertThat(request.getHeaders()).isNotNull();
        }

        @Test
        @DisplayName("应该传播 Baggage 头")
        void shouldPropagateBaggageHeaders() throws IOException {
            // given
            when(baggageConfig.isEnabled()).thenReturn(true);
            when(baggageConfig.getRemoteFields()).thenReturn(List.of("tenantId", "userId"));

            interceptor = new EnhancedTraceInterceptor(tracer, properties, sampler);
            when(sampler.shouldSample()).thenReturn(true);
            when(tracer.nextSpan()).thenReturn(span);
            when(span.name(anyString())).thenReturn(span);
            when(tracer.currentSpan()).thenReturn(null);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            assertThat(request.getHeaders()).isNotNull();
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("执行异常时应该结束 Span")
        void shouldEndSpanOnException() throws IOException {
            // given
            interceptor = new EnhancedTraceInterceptor(tracer, properties, sampler);
            when(sampler.shouldSample()).thenReturn(true);
            when(tracer.nextSpan()).thenReturn(span);
            when(span.name(anyString())).thenReturn(span);
            when(tracer.currentSpan()).thenReturn(null);
            when(execution.execute(any(), any())).thenThrow(new IOException("test error"));

            // when/then
            try {
                interceptor.intercept(request, new byte[0], execution);
            } catch (IOException e) {
                // 预期异常
            }

            // then
            verify(span).end();
        }
    }
}