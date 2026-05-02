package io.github.afgprojects.framework.core.web.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
import org.springframework.web.context.request.RequestContextHolder;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * TraceContext 单元测试
 */
@DisplayName("TraceContext 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TraceContextTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private io.micrometer.tracing.TraceContext micrometerTraceContext;

    @BeforeEach
    void setUp() {
        MDC.clear();
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
        TraceContext.setTracer(null);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
        TraceContext.setTracer(null);
    }

    @Nested
    @DisplayName("Tracer 管理测试")
    class TracerManagementTests {

        @Test
        @DisplayName("应该设置和获取 Tracer")
        void shouldSetAndGetTracer() {
            // when
            TraceContext.setTracer(tracer);

            // then
            assertThat(TraceContext.getTracer()).isEqualTo(tracer);
        }

        @Test
        @DisplayName("应该清除 Tracer")
        void shouldClearTracer() {
            // given
            TraceContext.setTracer(tracer);

            // when
            TraceContext.setTracer(null);

            // then
            assertThat(TraceContext.getTracer()).isNull();
        }
    }

    @Nested
    @DisplayName("TraceId 测试")
    class TraceIdTests {

        @Test
        @DisplayName("应该生成有效的 TraceId")
        void shouldGenerateValidTraceId() {
            // when
            String traceId = TraceContext.generateTraceId();

            // then
            assertThat(traceId).isNotNull();
            assertThat(traceId).hasSize(32);
            assertThat(traceId).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("应该生成唯一的 TraceId")
        void shouldGenerateUniqueTraceId() {
            // when
            String traceId1 = TraceContext.generateTraceId();
            String traceId2 = TraceContext.generateTraceId();

            // then
            assertThat(traceId1).isNotEqualTo(traceId2);
        }

        @Test
        @DisplayName("无 Tracer 时应该返回 null")
        void shouldReturnNullWhenNoTracer() {
            // when
            String traceId = TraceContext.getTraceId();

            // then
            assertThat(traceId).isNull();
        }
    }

    @Nested
    @DisplayName("RequestId 测试")
    class RequestIdTests {

        @Test
        @DisplayName("应该生成有效的 RequestId")
        void shouldGenerateValidRequestId() {
            // when
            String requestId = TraceContext.generateRequestId();

            // then
            assertThat(requestId).isNotNull();
            assertThat(requestId).hasSize(32);
            assertThat(requestId).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("应该生成唯一的 RequestId")
        void shouldGenerateUniqueRequestId() {
            // when
            String requestId1 = TraceContext.generateRequestId();
            String requestId2 = TraceContext.generateRequestId();

            // then
            assertThat(requestId1).isNotEqualTo(requestId2);
        }
    }

    @Nested
    @DisplayName("清除测试")
    class ClearTests {

        @Test
        @DisplayName("应该清除上下文和 MDC")
        void shouldClearContextAndMdc() {
            // given
            MDC.put("traceId", "test-trace");
            MDC.put("requestId", "test-request");

            // when
            TraceContext.clear();

            // then
            assertThat(MDC.get("traceId")).isNull();
            assertThat(MDC.get("requestId")).isNull();
        }
    }
}