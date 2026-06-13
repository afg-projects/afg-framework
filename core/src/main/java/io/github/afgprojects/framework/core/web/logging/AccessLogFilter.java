package io.github.afgprojects.framework.core.web.logging;

import java.io.IOException;
import java.util.List;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * 访问日志过滤器
 * <p>
 * 拦截所有 HTTP 请求，记录 method、path、status、duration、userId、tenantId、clientIp 等信息。
 * 支持配置排除路径（Ant 风格模式匹配）和慢请求阈值标记。
 * <p>
 * 日志格式示例：
 * <pre>
 * AccessLog | method=POST path=/users status=201 duration=125ms userId=1 tenantId=tenant-1 clientIp=192.168.1.1
 * AccessLog | method=GET path=/api/orders status=200 duration=3500ms SLOW userId=1 tenantId=default clientIp=10.0.0.1
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class AccessLogFilter extends OncePerRequestFilter {

    private final AfgCoreProperties properties;
    private final AntPathMatcher pathMatcher;

    /**
     * 创建访问日志过滤器。
     *
     * @param properties 核心配置属性
     */
    public AccessLogFilter(AfgCoreProperties properties) {
        this.properties = properties;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 检查排除路径
        String requestPath = request.getRequestURI();
        if (isExcluded(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logAccess(request, response, duration);
        }
    }

    /**
     * 检查请求路径是否在排除列表中。
     */
    private boolean isExcluded(String requestPath) {
        List<String> excludePaths = properties.getAccessLog().getExcludePaths();
        if (excludePaths == null || excludePaths.isEmpty()) {
            return false;
        }
        for (String pattern : excludePaths) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 记录访问日志。
     */
    private void logAccess(HttpServletRequest request, HttpServletResponse response, long duration) {
        String method = request.getMethod();
        String path = buildRequestPath(request);
        int status = response.getStatus();
        String userId = resolveUserId();
        String tenantId = resolveTenantId();
        String clientIp = resolveClientIp();

        StringBuilder logBuilder = new StringBuilder(128);
        logBuilder.append("AccessLog | method=").append(method);
        logBuilder.append(" path=").append(path);
        logBuilder.append(" status=").append(status);
        logBuilder.append(" duration=").append(duration).append("ms");

        if (duration > properties.getAccessLog().getSlowRequestThreshold()) {
            logBuilder.append(" SLOW");
        }

        logBuilder.append(" userId=").append(userId);
        logBuilder.append(" tenantId=").append(tenantId);

        if (properties.getAccessLog().isIncludeClientIp()) {
            logBuilder.append(" clientIp=").append(clientIp);
        }

        log.info(logBuilder.toString());
    }

    /**
     * 构建请求路径（可选包含查询字符串）。
     */
    private String buildRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (properties.getAccessLog().isIncludeQueryString()) {
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isBlank()) {
                return uri + "?" + queryString;
            }
        }
        return uri;
    }

    /**
     * 从请求上下文中解析用户 ID。
     */
    private String resolveUserId() {
        RequestContext context = AfgRequestContextHolder.getContext();
        if (context != null && context.getUserId() != null) {
            return String.valueOf(context.getUserId());
        }
        return "N/A";
    }

    /**
     * 从请求上下文中解析租户 ID。
     */
    private String resolveTenantId() {
        RequestContext context = AfgRequestContextHolder.getContext();
        if (context != null && context.getTenantId() != null) {
            return String.valueOf(context.getTenantId());
        }
        return "N/A";
    }

    /**
     * 从请求上下文中解析客户端 IP。
     */
    private String resolveClientIp() {
        RequestContext context = AfgRequestContextHolder.getContext();
        if (context != null && context.getClientIp() != null) {
            return context.getClientIp();
        }
        return "N/A";
    }
}
