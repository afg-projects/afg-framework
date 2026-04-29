package io.github.afgprojects.framework.core.trace;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.NonNull;

/**
 * Baggage 拦截器
 * <p>
 * 从 HTTP 请求头中提取 Baggage 信息并设置到上下文中。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>自动提取 X-Baggage-* 请求头</li>
 *   <li>设置到 BaggageContext</li>
 *   <li>支持自定义字段名称映射</li>
 * </ul>
 */
public class BaggageInterceptor implements Filter {

    /** Baggage 请求头前缀 */
    private static final String BAGGAGE_HEADER_PREFIX = "X-Baggage-";

    /** 标准 Baggage 头 */
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public void doFilter(@NonNull ServletRequest request, @NonNull ServletResponse response, @NonNull FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            extractBaggage(httpRequest);
        }

        chain.doFilter(request, response);
    }

    /**
     * 从请求中提取 Baggage
     */
    private void extractBaggage(HttpServletRequest request) {
        // 提取标准头
        extractTenantId(request);
        extractUserId(request);

        // 提取自定义 Baggage 头
        extractCustomBaggage(request);
    }

    /**
     * 提取租户ID
     */
    private void extractTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader(TENANT_ID_HEADER);
        if (tenantId == null) {
            tenantId = request.getHeader(BAGGAGE_HEADER_PREFIX + BaggageContext.TENANT_ID);
        }
        if (tenantId != null && !tenantId.isEmpty()) {
            BaggageContext.setTenantId(tenantId);
        }
    }

    /**
     * 提取用户ID
     */
    private void extractUserId(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null) {
            userId = request.getHeader(BAGGAGE_HEADER_PREFIX + BaggageContext.USER_ID);
        }
        if (userId != null && !userId.isEmpty()) {
            BaggageContext.setUserId(userId);
        }
    }

    /**
     * 提取自定义 Baggage 头
     */
    private void extractCustomBaggage(HttpServletRequest request) {
        // 遍历所有请求头，提取 X-Baggage-* 格式的头
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.startsWith(BAGGAGE_HEADER_PREFIX)) {
                String key = headerName.substring(BAGGAGE_HEADER_PREFIX.length());
                String value = request.getHeader(headerName);
                if (value != null && !value.isEmpty()) {
                    BaggageContext.set(key, value);
                }
            }
        }
    }
}