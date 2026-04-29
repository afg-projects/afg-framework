package io.github.afgprojects.framework.core.web.logging;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.github.afgprojects.framework.core.web.context.RequestContextFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;
import io.github.afgprojects.framework.core.web.trace.TraceContext;

/**
 * MDC 过滤器
 * <p>
 * 从 RequestContext 提取业务字段写入 MDC，用于结构化日志。
 * <p>
 * <strong>注意：</strong>当启用 Micrometer Tracing 时，traceId 由 Observation API 自动注入 MDC。
 * 本过滤器专注于业务字段（tenantId, userId, clientIp 等），并可以作为 traceId 的后备方案。
 * <p>
 * <h3>Micrometer Observation 迁移说明</h3>
 * <p>
 * Spring Boot 3.x+ 内置 Micrometer Observation API，提供自动 traceId 生成和传播：
 * <ul>
 *   <li>配置 {@code management.tracing.enabled=true} 启用自动追踪</li>
 *   <li>traceId 自动注入 MDC（由 ObservationRegistry 管理）</li>
 *   <li>支持 Zipkin、Jaeger、OTLP 等多种追踪后端</li>
 * </ul>
 * <p>
 * 迁移步骤：
 * <ol>
 *   <li>添加 {@code micrometer-tracing-bridge-brave} 或 {@code micrometer-tracing-bridge-otel} 依赖</li>
 *   <li>配置 {@code management.tracing.sampling.probability=1.0}</li>
 *   <li>移除 RequestContextFilter 中的手动 traceId 处理逻辑</li>
 *   <li>保留 MdcFilter 仅用于业务字段</li>
 * </ol>
 *
 * @see RequestContextFilter
 */
public class MdcFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(MdcFilter.class);

    // 追踪字段 - 由 Micrometer Tracing 或 RequestContextFilter 管理，此处作为后备
    private static final String FIELD_TRACE_ID = "traceId";

    // 业务字段 - 由 MdcFilter 管理
    private static final String FIELD_TENANT_ID = "tenantId";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_CLIENT_IP = "clientIp";
    private static final String FIELD_REQUEST_PATH = "requestPath";
    private static final String FIELD_REQUEST_METHOD = "requestMethod";
    private static final String FIELD_REQUEST_ID = "requestId";

    // 支持的所有字段
    private static final Set<String> ALL_SUPPORTED_FIELDS = Set.of(
            FIELD_TRACE_ID,
            FIELD_TENANT_ID,
            FIELD_USER_ID,
            FIELD_USERNAME,
            FIELD_CLIENT_IP,
            FIELD_REQUEST_PATH,
            FIELD_REQUEST_METHOD,
            FIELD_REQUEST_ID);

    private final Set<String> enabledFields;

    public MdcFilter(@NonNull LoggingProperties properties) {
        this.enabledFields = new HashSet<>(Arrays.asList(properties.getMdc().getFields()));
        // 验证配置的字段是否支持
        for (String field : enabledFields) {
            if (!ALL_SUPPORTED_FIELDS.contains(field)) {
                LOG.warn("Unknown MDC field configured: {}. Supported fields: {}", field, ALL_SUPPORTED_FIELDS);
            }
        }
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            RequestContext context = AfgRequestContextHolder.getContext();
            if (context != null) {
                populateMdc(context);
            }
            filterChain.doFilter(request, response);
        } finally {
            clearMdc();
        }
    }

    private void populateMdc(RequestContext context) {
        // traceId - 优先从 TraceContext 获取（Micrometer Tracing），作为后备设置到 MDC
        putIfEnabled(FIELD_TRACE_ID, TraceContext.getTraceId());

        // 业务字段
        putIfEnabled(FIELD_TENANT_ID, context.getTenantId() != null ? String.valueOf(context.getTenantId()) : null);
        putIfEnabled(FIELD_USER_ID, context.getUserId() != null ? String.valueOf(context.getUserId()) : null);
        putIfEnabled(FIELD_USERNAME, context.getUsername());
        putIfEnabled(FIELD_CLIENT_IP, context.getClientIp());
        putIfEnabled(FIELD_REQUEST_PATH, context.getRequestPath());
        putIfEnabled(FIELD_REQUEST_METHOD, context.getRequestMethod());
        putIfEnabled(FIELD_REQUEST_ID, context.getRequestId());
    }

    private void putIfEnabled(String field, String value) {
        if (enabledFields.contains(field) && value != null) {
            // 只在 MDC 中不存在时设置（避免覆盖 Micrometer Tracing 设置的值）
            if (MDC.get(field) == null) {
                MDC.put(field, value);
            }
        }
    }

    private void clearMdc() {
        for (String field : enabledFields) {
            MDC.remove(field);
        }
    }
}
