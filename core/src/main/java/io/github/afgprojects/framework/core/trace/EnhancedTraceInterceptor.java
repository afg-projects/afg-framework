package io.github.afgprojects.framework.core.trace;

import java.io.IOException;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import io.github.afgprojects.framework.core.client.TraceInterceptor;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * 增强的 HTTP 请求拦截器
 * <p>
 * 扩展 {@link TraceInterceptor}，支持 Baggage 传播和 Span 创建。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>传递 TraceId、RequestId、TenantId</li>
 *   <li>传递 Baggage 上下文（支持自定义字段）</li>
 *   <li>自动创建 HTTP 客户端 Span</li>
 *   <li>支持采样策略</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 使用默认配置
 * EnhancedTraceInterceptor interceptor = new EnhancedTraceInterceptor(tracer);
 *
 * // 使用自定义配置
 * EnhancedTraceInterceptor interceptor = new EnhancedTraceInterceptor(
 *     tracer, properties, sampler
 * );
 * }</pre>
 */
public class EnhancedTraceInterceptor extends TraceInterceptor {

    /** Baggage 请求头前缀 */
    private static final String BAGGAGE_HEADER_PREFIX = "X-Baggage-";

    private final @Nullable Tracer tracer;
    private final @Nullable TracingProperties properties;
    private final @Nullable TracingSampler sampler;

    /**
     * 创建增强的 TraceInterceptor（无 Micrometer Tracer）
     */
    public EnhancedTraceInterceptor() {
        this(null, null, null);
    }

    /**
     * 创建增强的 TraceInterceptor（使用 Micrometer Tracer）
     *
     * @param tracer Micrometer Tracer（可为 null）
     */
    public EnhancedTraceInterceptor(@Nullable Tracer tracer) {
        this(tracer, null, null);
    }

    /**
     * 创建增强的 TraceInterceptor（完整配置）
     *
     * @param tracer     Micrometer Tracer（可为 null）
     * @param properties 追踪配置属性（可为 null）
     * @param sampler    追踪采样器（可为 null）
     */
    public EnhancedTraceInterceptor(
            @Nullable Tracer tracer, @Nullable TracingProperties properties, @Nullable TracingSampler sampler) {
        super(tracer);
        this.tracer = tracer;
        this.properties = properties;
        this.sampler = sampler;
    }

    @Override
    public @NonNull ClientHttpResponse intercept(
            @NonNull HttpRequest request, byte @NonNull [] body, @NonNull ClientHttpRequestExecution execution)
            throws IOException {

        // 采样检查
        if (sampler != null && !sampler.shouldSample()) {
            return execution.execute(request, body);
        }

        // 创建 HTTP 客户端 Span
        Span span = null;
        if (tracer != null && (properties == null || properties.isEnabled())) {
            span = createHttpClientSpan(request);
        }

        try {
            // 传递标准追踪头
            propagateStandardHeaders(request);

            // 传递 Baggage
            if (properties != null && properties.getBaggage().isEnabled()) {
                propagateBaggageHeaders(request);
            }

            // 执行请求
            ClientHttpResponse response = execution.execute(request, body);

            // 记录响应状态
            if (span != null) {
                span.tag("http.status_code", String.valueOf(response.getStatusCode().value()));
            }

            return response;

        } catch (IOException ex) {
            // 记录异常
            if (span != null) {
                span.tag("exception", ex.getClass().getSimpleName());
                span.tag("exception.message", ex.getMessage());
            }
            throw ex;

        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    /**
     * 创建 HTTP 客户端 Span
     */
    private @Nullable Span createHttpClientSpan(HttpRequest request) {
        if (tracer == null) {
            return null;
        }

        Span currentSpan = tracer.currentSpan();
        Span span;

        if (currentSpan != null) {
            // 作为子 Span
            span = tracer.spanBuilder()
                    .setParent(currentSpan.context())
                    .name("http.client." + extractPath(request))
                    .start();
        } else {
            // 创建新 Span
            span = tracer.nextSpan().name("http.client." + extractPath(request));
            span.start();
        }

        // 设置 HTTP 相关标签
        span.tag("span.kind", "client");
        span.tag("http.method", request.getMethod().name());
        span.tag("http.url", request.getURI().toString());

        return span;
    }

    /**
     * 提取请求路径
     */
    private String extractPath(HttpRequest request) {
        String path = request.getURI().getPath();
        return path != null && !path.isEmpty() ? path : "/";
    }

    /**
     * 传播标准追踪头
     */
    private void propagateStandardHeaders(HttpRequest request) {
        // 传递 TraceId
        String traceId = TraceContext.getTraceId();
        if (traceId != null) {
            request.getHeaders().add("X-Trace-Id", traceId);
        }

        // 传递 RequestId
        String requestId = TraceContext.generateRequestId();
        request.getHeaders().add("X-Request-Id", requestId);

        // 传递租户ID
        Long tenantId = AfgRequestContextHolder.getTenantId();
        if (tenantId != null) {
            request.getHeaders().add("X-Tenant-Id", String.valueOf(tenantId));
        }

        // 传递用户ID
        Long userId = AfgRequestContextHolder.getUserId();
        if (userId != null) {
            request.getHeaders().add("X-User-Id", String.valueOf(userId));
        }
    }

    /**
     * 传播 Baggage 头
     */
    private void propagateBaggageHeaders(HttpRequest request) {
        Map<String, String> baggage = BaggageContext.getAll();

        if (properties == null) {
            // 没有配置时，传播所有 baggage
            baggage.forEach((key, value) -> {
                if (value != null) {
                    request.getHeaders().add(BAGGAGE_HEADER_PREFIX + key, value);
                }
            });
        } else {
            // 使用配置的远程传播字段
            for (String field : properties.getBaggage().getRemoteFields()) {
                String value = baggage.get(field);
                if (value != null) {
                    String headerName = getBaggageHeaderName(field);
                    request.getHeaders().add(headerName, value);
                }
            }
        }
    }

    /**
     * 获取 Baggage 请求头名称
     */
    private String getBaggageHeaderName(String field) {
        if (properties == null) {
            return BAGGAGE_HEADER_PREFIX + field;
        }

        // 检查是否有自定义映射
        String mappedName = properties.getBaggage().getFieldMappings().get(field);
        return mappedName != null ? mappedName : BAGGAGE_HEADER_PREFIX + field;
    }
}