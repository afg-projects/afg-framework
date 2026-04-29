package io.github.afgprojects.framework.core.trace;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * Span 创建器
 * <p>
 * 负责创建和管理 Micrometer Tracing Span
 * </p>
 */
class SpanCreator {

    private final @Nullable Tracer tracer;

    /**
     * 构造函数
     *
     * @param tracer Micrometer Tracer（可为 null）
     */
    SpanCreator(@Nullable Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * 创建 Span
     *
     * @param kind          Span 类型
     * @param operationName 操作名称
     * @return 创建的 Span
     * @throws IllegalStateException 如果 tracer 为 null
     */
    @NonNull Span createSpan(@NonNull SpanKind kind, @NonNull String operationName) {
        if (tracer == null) {
            throw new IllegalStateException("Tracer is not available");
        }

        Span currentSpan = tracer.currentSpan();
        Span span;

        if (currentSpan != null) {
            // 作为子 Span
            span = tracer.spanBuilder()
                    .setParent(currentSpan.context())
                    .name(operationName)
                    .start();
        } else {
            // 创建新 Span
            span = tracer.nextSpan().name(operationName);
            span.start();
        }

        // 设置 Span 类型标签
        span.tag("span.kind", kind.name().toLowerCase());

        // 关联 traceId 到 MDC
        String traceId = span.context().traceId();
        if (traceId != null) {
            TraceContext.setTraceId(traceId);
        }

        return span;
    }
}
