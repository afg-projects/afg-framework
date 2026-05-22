package io.github.afgprojects.framework.ai.observability;

import io.github.afgprojects.framework.ai.core.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.observability.Tracer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring AI Observation 适配器测试
 */
class SpringAiObservationAdapterTest {

    private ObservationRegistry observationRegistry;
    private MeterRegistry meterRegistry;
    private SpringAiObservationAdapter adapter;

    @BeforeEach
    void setUp() {
        observationRegistry = ObservationRegistry.create();
        meterRegistry = new SimpleMeterRegistry();
        adapter = new SpringAiObservationAdapter(observationRegistry, meterRegistry);
    }

    @Nested
    @DisplayName("Tracer 接口测试")
    class TracerTests {

        @Test
        @DisplayName("创建 Span")
        void shouldStartSpan() {
            Tracer.Span span = adapter.startSpan("test-operation");

            assertThat(span).isNotNull();
            assertThat(span.getOperationName()).isEqualTo("test-operation");
            assertThat(span.getTraceId()).isNotBlank();
            assertThat(span.getSpanId()).isNotBlank();
            assertThat(span.isEnded()).isFalse();
        }

        @Test
        @DisplayName("创建带父 Span 的 Span")
        void shouldStartSpanWithParent() {
            Tracer.Span parentSpan = adapter.startSpan("parent-operation");
            Tracer.Span childSpan = adapter.startSpan("child-operation", parentSpan);

            assertThat(childSpan.getTraceId()).isEqualTo(parentSpan.getTraceId());
            assertThat(childSpan.getContext().getParentSpanId()).isEqualTo(parentSpan.getSpanId());
        }

        @Test
        @DisplayName("设置 Span 属性")
        void shouldSetSpanAttributes() {
            Tracer.Span span = adapter.startSpan("test-operation");

            span.setAttribute("string-key", "value");
            span.setAttribute("long-key", 123L);
            span.setAttribute("boolean-key", true);

            // 属性设置不应抛出异常
            assertThat(span.isEnded()).isFalse();
        }

        @Test
        @DisplayName("记录事件")
        void shouldRecordEvent() {
            Tracer.Span span = adapter.startSpan("test-operation");

            span.recordEvent("test-event");
            span.recordEvent("test-event-with-attrs", Map.of("key", "value"));

            // 事件记录不应抛出异常
            assertThat(span.isEnded()).isFalse();
        }

        @Test
        @DisplayName("记录异常")
        void shouldRecordException() {
            Tracer.Span span = adapter.startSpan("test-operation");

            span.recordException(new RuntimeException("Test exception"));

            assertThat(span.isEnded()).isFalse();
        }

        @Test
        @DisplayName("结束 Span")
        void shouldEndSpan() {
            Tracer.Span span = adapter.startSpan("test-operation");

            span.end(Tracer.SpanStatus.OK);

            assertThat(span.isEnded()).isTrue();
        }

        @Test
        @DisplayName("提取和注入上下文")
        void shouldExtractAndInjectContext() {
            Tracer.Span span = adapter.startSpan("test-operation");
            span.getContext().setBaggage("userId", "user-123");

            Map<String, String> headers = adapter.injectContext(span.getContext());

            assertThat(headers).containsKey("X-Trace-Id");
            assertThat(headers).containsKey("X-Span-Id");
            assertThat(headers).containsEntry("X-Baggage-userId", "user-123");

            Tracer.TraceContext extractedContext = adapter.extractContext(headers);
            assertThat(extractedContext.getTraceId()).isEqualTo(span.getTraceId());
            assertThat(extractedContext.getBaggage("userId")).isEqualTo("user-123");
        }

        @Test
        @DisplayName("获取当前 Span")
        void shouldGetCurrentSpan() {
            Tracer.Span span = adapter.startSpan("test-operation");

            assertThat(adapter.getCurrentSpan()).isEqualTo(span);

            span.end();

            // 结束后当前 Span 应被清除
            assertThat(adapter.getCurrentSpan()).isNull();
        }
    }

    @Nested
    @DisplayName("MetricsCollector 接口测试")
    class MetricsCollectorTests {

        @Test
        @DisplayName("记录请求计数")
        void shouldRecordCount() {
            adapter.recordCount("chat", "gpt-4", "success", Map.of("tenant", "test"));

            assertThat(meterRegistry.counter("afg.ai.chat.requests", "model", "gpt-4", "status", "success", "tenant", "test").count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("记录 Token 使用量")
        void shouldRecordTokenUsage() {
            adapter.recordTokenUsage("gpt-4", 100, 50, Map.of());

            assertThat(meterRegistry.counter("afg.ai.tokens.input", "model", "gpt-4").count()).isEqualTo(100.0);
            assertThat(meterRegistry.counter("afg.ai.tokens.output", "model", "gpt-4").count()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("记录成本")
        void shouldRecordCost() {
            adapter.recordCost("gpt-4", 0.05, Map.of());

            assertThat(meterRegistry.counter("afg.ai.cost", "model", "gpt-4").count()).isEqualTo(0.05);
        }

        @Test
        @DisplayName("记录响应大小")
        void shouldRecordResponseSize() {
            adapter.recordResponseSize("chat", "gpt-4", 1024, Map.of());

            assertThat(meterRegistry.summary("afg.ai.chat.response.size", "model", "gpt-4").totalAmount()).isEqualTo(1024.0);
        }

        @Test
        @DisplayName("使用计时器")
        void shouldUseTimer() {
            MetricsCollector.Timer timer = adapter.startTimer("chat", "gpt-4", Map.of());

            assertThat(timer.getStartTimeMs()).isPositive();
            assertThat(timer.getElapsed()).isNotNull();

            timer.stop("success");

            assertThat(meterRegistry.timer("afg.ai.chat.duration", "model", "gpt-4", "status", "success").count()).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("获取指标摘要")
        void shouldGetSummary() {
            MetricsCollector.MetricsSummary summary = adapter.getSummary();

            // Micrometer 适配器不提供聚合数据
            assertThat(summary.getTotalRequests()).isEqualTo(0);
            assertThat(summary.getModelStats()).isEmpty();
        }
    }

    @Nested
    @DisplayName("无 MeterRegistry 测试")
    class NoMeterRegistryTests {

        private SpringAiObservationAdapter adapterWithoutMeterRegistry;

        @BeforeEach
        void setUp() {
            adapterWithoutMeterRegistry = new SpringAiObservationAdapter(observationRegistry, null);
        }

        @Test
        @DisplayName("无 MeterRegistry 时记录指标不抛出异常")
        void shouldNotThrowWhenMeterRegistryIsNull() {
            // 这些操作不应抛出异常
            adapterWithoutMeterRegistry.recordCount("chat", "gpt-4", "success", Map.of());
            adapterWithoutMeterRegistry.recordTokenUsage("gpt-4", 100, 50, Map.of());
            adapterWithoutMeterRegistry.recordCost("gpt-4", 0.05, Map.of());
            adapterWithoutMeterRegistry.recordResponseSize("chat", "gpt-4", 1024, Map.of());

            MetricsCollector.Timer timer = adapterWithoutMeterRegistry.startTimer("chat", "gpt-4", Map.of());
            timer.stop("success");
        }
    }
}