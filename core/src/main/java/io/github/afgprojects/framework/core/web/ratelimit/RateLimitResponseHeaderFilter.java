package io.github.afgprojects.framework.core.web.ratelimit;

import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 限流响应头过滤器
 * <p>
 * 在响应头中添加限流信息，包括：
 * - X-RateLimit-Limit: 限流阈值
 * - X-RateLimit-Remaining: 剩余配额
 * - X-RateLimit-Reset: 重置时间
 * - Retry-After: 重试等待时间（仅限流时）
 * </p>
 */
public class RateLimitResponseHeaderFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;

    /**
     * 请求属性 key，用于存储限流结果
     */
    public static final String RATE_LIMIT_RESULT_ATTR = RateLimitResult.class.getName();

    /**
     * 构造函数
     *
     * @param properties 限流配置属性
     */
    public RateLimitResponseHeaderFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        // 检查是否启用响应头
        if (!properties.getResponseHeaders().isEnabled()) {
            return;
        }

        // 获取限流结果
        RateLimitResult result = (RateLimitResult) request.getAttribute(RATE_LIMIT_RESULT_ATTR);
        if (result == null) {
            return;
        }

        // 设置响应头
        var headers = properties.getResponseHeaders();
        response.setHeader(headers.getLimitHeader(), result.getLimitHeader());
        response.setHeader(headers.getRemainingHeader(), result.getRemainingHeader());
        response.setHeader(headers.getResetHeader(), result.getResetHeader());

        // 仅在限流时设置 Retry-After
        if (!result.allowed()) {
            response.setHeader(headers.getRetryAfterHeader(), result.getRetryAfterHeader());
        }
    }

    /**
     * 设置限流结果到请求属性
     *
     * @param request HTTP 请求
     * @param result  限流结果
     */
    public static void setRateLimitResult(HttpServletRequest request, @Nullable RateLimitResult result) {
        if (result != null) {
            request.setAttribute(RATE_LIMIT_RESULT_ATTR, result);
        }
    }
}
