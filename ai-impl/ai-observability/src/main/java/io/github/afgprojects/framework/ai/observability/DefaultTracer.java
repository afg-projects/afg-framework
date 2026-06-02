package io.github.afgprojects.framework.ai.observability;

import io.github.afgprojects.framework.ai.core.api.observability.Tracer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认追踪器实现
 *
 * <p>基于内存的简单追踪器，适用于：
 * <ul>
 *   <li>开发测试环境</li>
 *   <li>不需要分布式追踪的场景</li>
 *   <li>日志输出追踪信息</li>
 * </ul>
 *
 * <p>生产环境建议使用 OpenTelemetry 或 Jaeger 实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultTracer implements Tracer {

    private static final Logger log = LoggerFactory.getLogger(DefaultTracer.class);

    private final AtomicReference<Span> currentSpan = new AtomicReference<>();

    @Override
    @NonNull
    public Span startSpan(@NonNull String operationName) {
        return startSpan(operationName, (Span) null);
    }

    @Override
    @NonNull
    public Span startSpan(@NonNull String operationName, @Nullable Span parentSpan) {
        String traceId = parentSpan != null ? parentSpan.getTraceId() : generateTraceId();
        String parentSpanId = parentSpan != null ? parentSpan.getSpanId() : null;

        DefaultSpan span = new DefaultSpan(operationName, traceId, generateSpanId(), parentSpanId);

        if (parentSpan instanceof DefaultSpan) {
            span.context.setAllBaggage(((DefaultSpan) parentSpan).context.getAllBaggage());
        }

        currentSpan.set(span);
        log.debug("Started span: {} (traceId={}, spanId={})", operationName, traceId, span.getSpanId());

        return span;
    }

    @Override
    @NonNull
    public Span startSpanWithContext(@NonNull String operationName, @NonNull TraceContext context) {
        DefaultSpan span = new DefaultSpan(operationName, context.getTraceId(),
                generateSpanId(), context.getSpanId());
        span.context.setAllBaggage(context.getAllBaggage());

        currentSpan.set(span);
        log.debug("Started span with context: {} (traceId={}, spanId={})",
                operationName, span.getTraceId(), span.getSpanId());

        return span;
    }

    @Override
    @Nullable
    public Span getCurrentSpan() {
        return currentSpan.get();
    }

    @Override
    @NonNull
    public TraceContext extractContext(@NonNull Map<String, String> headers) {
        String traceId = headers.getOrDefault("X-Trace-Id", generateTraceId());
        String spanId = headers.getOrDefault("X-Span-Id", generateSpanId());
        String parentSpanId = headers.get("X-Parent-Span-Id");

        DefaultTraceContext context = new DefaultTraceContext(traceId, spanId, parentSpanId);

        // 提取 baggage
        headers.entrySet().stream()
                .filter(e -> e.getKey().startsWith("X-Baggage-"))
                .forEach(e -> context.setBaggage(
                        e.getKey().substring("X-Baggage-".length()),
                        e.getValue()
                ));

        return context;
    }

    @Override
    @NonNull
    public Map<String, String> injectContext(@NonNull TraceContext context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Trace-Id", context.getTraceId());
        headers.put("X-Span-Id", context.getSpanId());

        if (context.getParentSpanId() != null) {
            headers.put("X-Parent-Span-Id", context.getParentSpanId());
        }

        context.getAllBaggage().forEach((k, v) -> headers.put("X-Baggage-" + k, v));

        return headers;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 默认 Span 实现
     */
    private class DefaultSpan implements Span {

        private final String operationName;
        private final String traceId;
        private final String spanId;
        private final String parentSpanId;
        private final DefaultTraceContext context;
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private final long startTimeMs;

        private volatile SpanStatus status = SpanStatus.UNSET;
        private volatile boolean ended = false;

        DefaultSpan(String operationName, String traceId, String spanId, String parentSpanId) {
            this.operationName = operationName;
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
            this.context = new DefaultTraceContext(traceId, spanId, parentSpanId);
            this.startTimeMs = System.currentTimeMillis();
        }

        @Override
        @NonNull
        public Span setAttribute(@NonNull String key, @NonNull String value) {
            attributes.put(key, value);
            return this;
        }

        @Override
        @NonNull
        public Span setAttribute(@NonNull String key, long value) {
            attributes.put(key, value);
            return this;
        }

        @Override
        @NonNull
        public Span setAttribute(@NonNull String key, boolean value) {
            attributes.put(key, value);
            return this;
        }

        @Override
        public void recordEvent(@NonNull String name) {
            recordEvent(name, Map.of());
        }

        @Override
        public void recordEvent(@NonNull String name, @NonNull Map<String, String> eventAttributes) {
            log.debug("Event recorded on span {}: {} (attributes: {})", spanId, name, eventAttributes);
        }

        @Override
        public void recordException(@NonNull Exception exception) {
            setAttribute("error.type", exception.getClass().getName());
            setAttribute("error.message", exception.getMessage());
            setStatus(SpanStatus.ERROR);
            log.debug("Exception recorded on span {}: {}", spanId, exception.getMessage());
        }

        @Override
        public void setStatus(@NonNull SpanStatus spanStatus) {
            this.status = spanStatus;
        }

        @Override
        public void end() {
            end(status);
        }

        @Override
        public void end(@NonNull SpanStatus endStatus) {
            if (ended) {
                return;
            }
            ended = true;
            this.status = endStatus;

            long durationMs = System.currentTimeMillis() - startTimeMs;
            log.debug("Ended span: {} (traceId={}, spanId={}, status={}, duration={}ms)",
                    operationName, traceId, spanId, endStatus, durationMs);

            // 清除当前 span
            currentSpan.compareAndSet(this, null);
        }

        @Override
        @NonNull
        public String getSpanId() {
            return spanId;
        }

        @Override
        @NonNull
        public String getTraceId() {
            return traceId;
        }

        @Override
        @NonNull
        public String getOperationName() {
            return operationName;
        }

        @Override
        @NonNull
        public TraceContext getContext() {
            return context;
        }

        @Override
        public boolean isEnded() {
            return ended;
        }
    }

    /**
     * 默认追踪上下文实现
     */
    private static class DefaultTraceContext implements TraceContext {

        private final String traceId;
        private final String spanId;
        private final String parentSpanId;
        private final Map<String, String> baggage = new ConcurrentHashMap<>();

        DefaultTraceContext(String traceId, String spanId, String parentSpanId) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
        }

        @Override
        @NonNull
        public String getTraceId() {
            return traceId;
        }

        @Override
        @NonNull
        public String getSpanId() {
            return spanId;
        }

        @Override
        @Nullable
        public String getParentSpanId() {
            return parentSpanId;
        }

        @Override
        @Nullable
        public String getBaggage(@NonNull String key) {
            return baggage.get(key);
        }

        @Override
        public void setBaggage(@NonNull String key, @NonNull String value) {
            baggage.put(key, value);
        }

        @Override
        @NonNull
        public Map<String, String> getAllBaggage() {
            return new HashMap<>(baggage);
        }

        void setAllBaggage(Map<String, String> allBaggage) {
            baggage.putAll(allBaggage);
        }
    }
}