package io.github.afgprojects.framework.core.web.context;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * 请求上下文过滤器
 * 在请求入口设置 traceId/requestId，并在响应中回传 traceId
 * <p>
 * 依赖 Spring RequestContextHolder 管理请求作用域数据
 * <p>
 * <h3>Micrometer Observation 集成</h3>
 * <p>
 * 当 {@link Tracer} 可用时，优先使用 Micrometer Tracing 管理 traceId：
 * <ul>
 *   <li>从当前 Span 获取 traceId（如果已由 Spring Boot 自动创建）</li>
 *   <li>如果无活跃 Span，创建新 Span 并提取 traceId</li>
 *   <li>traceId 自动传播到 MDC（由 Observation API 管理）</li>
 * </ul>
 * <p>
 * 当 Tracer 不可用时，回退到手动生成：
 * <ul>
 *   <li>从请求头或参数提取 traceId，否则生成新的</li>
 *   <li>手动设置 MDC</li>
 * </ul>
 */
public class RequestContextFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_PARAM = "traceId";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_REQUEST_ID = "requestId";

    private final @Nullable Tracer tracer;

    /**
     * 创建 RequestContextFilter（无 Micrometer Tracer）
     */
    public RequestContextFilter() {
        this(null);
    }

    /**
     * 创建 RequestContextFilter（使用 Micrometer Tracer）
     *
     * @param tracer Micrometer Tracer（可为 null）
     */
    public RequestContextFilter(@Nullable Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        RequestAttributes previousAttrs = prepareRequestAttributes();

        SpanInfo spanInfo = null;
        try {
            RequestContextHolder.setRequestAttributes(
                    previousAttrs instanceof ServletRequestAttributes
                            ? (ServletRequestAttributes) previousAttrs
                            : new ServletRequestAttributes(request, response));

            RequestContext context = RequestContext.builder().build();
            spanInfo = initializeTrace(request, context);
            String requestId = TraceContext.generateRequestId();

            context.setRequestId(requestId);
            AfgRequestContextHolder.setContext(context);
            MDC.put(MDC_REQUEST_ID, requestId);

            filterChain.doFilter(request, response);
        } finally {
            cleanup(response, spanInfo, previousAttrs);
        }
    }

    private RequestAttributes prepareRequestAttributes() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes ? attrs : null;
    }

    /**
     * 初始化追踪信息，返回 Span 信息（如果创建了新 Span）
     */
    private @Nullable SpanInfo initializeTrace(HttpServletRequest request, RequestContext context) {
        if (tracer != null) {
            return initializeWithTracer(request, context);
        } else {
            initializeManually(request, context);
            return null;
        }
    }

    /**
     * 使用 Micrometer Tracer 初始化追踪
     */
    private @Nullable SpanInfo initializeWithTracer(HttpServletRequest request, RequestContext context) {
        Span span = tracer.currentSpan();
        String traceId;

        if (span != null) {
            // 已有活跃 Span
            traceId = span.context().traceId();
            MDC.put(MDC_TRACE_ID, traceId);
            context.setTraceId(traceId);
            return null;
        }

        // 创建新 Span
        String incomingTraceId = extractIncomingTraceId(request);
        span = tracer.nextSpan().name("http.request");
        if (incomingTraceId != null) {
            span = span.tag("parent.traceId", incomingTraceId);
        }
        span.start();
        traceId = span.context().traceId();

        MDC.put(MDC_TRACE_ID, traceId);
        context.setTraceId(traceId);

        return new SpanInfo(span);
    }

    /**
     * 手动初始化追踪（无 Tracer 时）
     */
    private void initializeManually(HttpServletRequest request, RequestContext context) {
        String traceId = extractIncomingTraceId(request);
        if (traceId == null) {
            traceId = TraceContext.generateTraceId();
        }
        MDC.put(MDC_TRACE_ID, traceId);
        context.setTraceId(traceId);
    }

    /**
     * 从请求中提取传入的 traceId
     */
    private @Nullable String extractIncomingTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getParameter(TRACE_ID_PARAM);
        }
        return (traceId != null && !traceId.isBlank()) ? traceId : null;
    }

    /**
     * 清理资源和恢复状态
     */
    private void cleanup(
            HttpServletResponse response, @Nullable SpanInfo spanInfo, @Nullable RequestAttributes previousAttrs) {
        // 写入响应头
        String traceId = AfgRequestContextHolder.getTraceId();
        if (traceId != null && !response.isCommitted()) {
            response.setHeader(TRACE_ID_HEADER, traceId);
        }

        // 清理 MDC 和上下文
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_REQUEST_ID);
        AfgRequestContextHolder.clear();

        // 关闭 Span
        if (spanInfo != null && spanInfo.span != null) {
            spanInfo.span.end();
        }

        // 恢复 RequestAttributes
        if (previousAttrs != null) {
            RequestContextHolder.setRequestAttributes(previousAttrs);
        } else {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * Span 信息持有类
     */
    private static class SpanInfo {
        final Span span;

        SpanInfo(Span span) {
            this.span = span;
        }
    }
}
