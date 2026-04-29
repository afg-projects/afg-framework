package io.github.afgprojects.framework.core.web.trace;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;
import io.micrometer.tracing.Tracer;

/**
 * 链路追踪上下文工具类
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li>Micrometer Tracing 模式：从 {@link Tracer} 获取当前 Span 的 traceId</li>
 *   <li>手动模式：从 {@link AfgRequestContextHolder} 获取 traceId</li>
 * </ul>
 */
public final class TraceContext {

    private static final AtomicReference<@Nullable Tracer> TRACER_REF = new AtomicReference<>(null);

    private TraceContext() {}

    /**
     * 设置全局 Tracer（由框架自动调用）
     *
     * @param tracer Micrometer Tracer
     */
    public static void setTracer(@Nullable Tracer tracer) {
        TRACER_REF.set(tracer);
    }

    /**
     * 获取当前 Tracer
     *
     * @return 当前 Tracer，可能为 null
     */
    public static @Nullable Tracer getTracer() {
        return TRACER_REF.get();
    }

    /**
     * 获取当前 TraceId
     * <p>
     * 优先从 Micrometer Tracer 获取，其次从 AfgRequestContextHolder 获取
     */
    public static @Nullable String getTraceId() {
        // 优先使用 Micrometer Tracer
        Tracer tracer = TRACER_REF.get();
        if (tracer != null) {
            var span = tracer.currentSpan();
            if (span != null) {
                return span.context().traceId();
            }
        }
        // 回退到手动模式
        return AfgRequestContextHolder.getTraceId();
    }

    /**
     * 获取当前 RequestId
     */
    public static @Nullable String getRequestId() {
        return AfgRequestContextHolder.getRequestId();
    }

    /**
     * 设置 TraceId（手动模式）
     */
    public static void setTraceId(String traceId) {
        RequestContext context = AfgRequestContextHolder.getContext();
        if (context != null) {
            context.setTraceId(traceId);
        }
        // 同步到 MDC
        if (traceId != null) {
            MDC.put("traceId", traceId);
        } else {
            MDC.remove("traceId");
        }
    }

    /**
     * 设置 RequestId
     */
    public static void setRequestId(String requestId) {
        RequestContext context = AfgRequestContextHolder.getContext();
        if (context != null) {
            context.setRequestId(requestId);
        }
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        AfgRequestContextHolder.clear();
        // 清除 MDC
        MDC.clear();
    }

    /**
     * 生成新的 TraceId（32 字符 hex，基于 ThreadLocalRandom）
     */
    public static String generateTraceId() {
        return generateHexId();
    }

    /**
     * 生成新的 RequestId（32 字符 hex，基于 ThreadLocalRandom）
     */
    public static String generateRequestId() {
        return generateHexId();
    }

    private static String generateHexId() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long hi = rng.nextLong();
        long lo = rng.nextLong();
        return String.format("%016x%016x", hi, lo);
    }
}
