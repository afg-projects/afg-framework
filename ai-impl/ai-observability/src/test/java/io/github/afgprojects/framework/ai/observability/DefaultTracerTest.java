package io.github.afgprojects.framework.ai.observability;

import io.github.afgprojects.framework.ai.core.observability.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultTracer 单元测试
 */
class DefaultTracerTest {

    private DefaultTracer tracer;

    @BeforeEach
    void setUp() {
        tracer = new DefaultTracer();
    }

    @Test
    @DisplayName("创建 Span")
    void startSpan() {
        Tracer.Span span = tracer.startSpan("chat");

        assertThat(span.getOperationName()).isEqualTo("chat");
        assertThat(span.getTraceId()).isNotBlank();
        assertThat(span.getSpanId()).isNotBlank();
        assertThat(span.isEnded()).isFalse();
    }

    @Test
    @DisplayName("创建子 Span")
    void startSpan_withParent() {
        Tracer.Span parent = tracer.startSpan("chat");
        Tracer.Span child = tracer.startSpan("completion", parent);

        assertThat(child.getTraceId()).isEqualTo(parent.getTraceId());
        assertThat(child.getContext().getParentSpanId()).isEqualTo(parent.getSpanId());
    }

    @Test
    @DisplayName("设置 Span 属性")
    void setAttribute() {
        Tracer.Span span = tracer.startSpan("chat");

        span.setAttribute("model", "gpt-4");
        span.setAttribute("tokens", 100L);
        span.setAttribute("stream", true);

        // 属性设置成功（无异常）
        assertThat(span.isEnded()).isFalse();
    }

    @Test
    @DisplayName("记录事件")
    void recordEvent() {
        Tracer.Span span = tracer.startSpan("chat");

        span.recordEvent("request_sent");
        span.recordEvent("response_received", Map.of("tokens", "100"));

        // 事件记录成功（无异常）
    }

    @Test
    @DisplayName("记录异常")
    void recordException() {
        Tracer.Span span = tracer.startSpan("chat");

        span.recordException(new RuntimeException("timeout"));

        // 异常记录成功（无异常）
    }

    @Test
    @DisplayName("设置状态并结束")
    void setStatusAndEnd() {
        Tracer.Span span = tracer.startSpan("chat");

        span.setStatus(Tracer.SpanStatus.OK);
        span.end();

        assertThat(span.isEnded()).isTrue();
    }

    @Test
    @DisplayName("带状态结束")
    void end_withStatus() {
        Tracer.Span span = tracer.startSpan("chat");

        span.end(Tracer.SpanStatus.ERROR);

        assertThat(span.isEnded()).isTrue();
    }

    @Test
    @DisplayName("获取当前 Span")
    void getCurrentSpan() {
        Tracer.Span span = tracer.startSpan("chat");

        assertThat(tracer.getCurrentSpan()).isEqualTo(span);

        span.end();

        // 结束后当前 Span 被清除
        assertThat(tracer.getCurrentSpan()).isNull();
    }

    @Test
    @DisplayName("提取追踪上下文")
    void extractContext() {
        Map<String, String> headers = Map.of(
                "X-Trace-Id", "trace-123",
                "X-Span-Id", "span-456",
                "X-Parent-Span-Id", "parent-789",
                "X-Baggage-userId", "user-001"
        );

        Tracer.TraceContext context = tracer.extractContext(headers);

        assertThat(context.getTraceId()).isEqualTo("trace-123");
        assertThat(context.getSpanId()).isEqualTo("span-456");
        assertThat(context.getParentSpanId()).isEqualTo("parent-789");
        assertThat(context.getBaggage("userId")).isEqualTo("user-001");
    }

    @Test
    @DisplayName("注入追踪上下文")
    void injectContext() {
        Tracer.Span span = tracer.startSpan("chat");
        span.getContext().setBaggage("userId", "user-001");

        Map<String, String> headers = tracer.injectContext(span.getContext());

        assertThat(headers.get("X-Trace-Id")).isEqualTo(span.getTraceId());
        assertThat(headers.get("X-Span-Id")).isEqualTo(span.getSpanId());
        assertThat(headers.get("X-Baggage-userId")).isEqualTo("user-001");
    }

    @Test
    @DisplayName("Baggage 在 Span 间传递")
    void baggagePropagation() {
        Tracer.Span parent = tracer.startSpan("chat");
        parent.getContext().setBaggage("userId", "user-001");

        Tracer.Span child = tracer.startSpan("completion", parent);

        assertThat(child.getContext().getBaggage("userId")).isEqualTo("user-001");
    }

    @Test
    @DisplayName("使用上下文创建 Span")
    void startSpanWithContext() {
        Tracer.Span original = tracer.startSpan("chat");
        original.getContext().setBaggage("tenantId", "tenant-001");

        Tracer.TraceContext context = original.getContext();
        Tracer.Span newSpan = tracer.startSpanWithContext("completion", context);

        assertThat(newSpan.getTraceId()).isEqualTo(original.getTraceId());
        assertThat(newSpan.getContext().getBaggage("tenantId")).isEqualTo("tenant-001");
    }
}