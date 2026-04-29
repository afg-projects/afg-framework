package io.github.afgprojects.framework.core.client;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.Tracer;

/**
 * HTTP 请求拦截器
 * 传递链路追踪信息
 * <p>
 * 当 Micrometer Tracer 可用时，traceId 从当前 Span 获取；
 * 否则从 {@link TraceContext} 获取。
 */
public class TraceInterceptor implements ClientHttpRequestInterceptor {

    /** TraceId 请求头 */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** RequestId 请求头 */
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** 租户ID 请求头 */
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    private final @Nullable Tracer tracer;

    /**
     * 创建 TraceInterceptor（无 Micrometer Tracer）
     */
    public TraceInterceptor() {
        this.tracer = null;
    }

    /**
     * 创建 TraceInterceptor（使用 Micrometer Tracer）
     *
     * @param tracer Micrometer Tracer（可为 null）
     */
    public TraceInterceptor(@Nullable Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public @NonNull ClientHttpResponse intercept(
            @NonNull HttpRequest request, byte @NonNull [] body, @NonNull ClientHttpRequestExecution execution)
            throws IOException {
        // 传递 TraceId（优先使用 Micrometer Tracer）
        String traceId = TraceContext.getTraceId();
        if (traceId != null) {
            request.getHeaders().add(TRACE_ID_HEADER, traceId);
        }

        // 传递 RequestId（生成新的子请求ID）
        String requestId = TraceContext.generateRequestId();
        request.getHeaders().add(REQUEST_ID_HEADER, requestId);

        // 传递租户ID
        Long tenantId = AfgRequestContextHolder.getTenantId();
        if (tenantId != null) {
            request.getHeaders().add(TENANT_ID_HEADER, String.valueOf(tenantId));
        }

        return execution.execute(request, body);
    }
}
