package io.github.afgprojects.framework.security.auth.tenant.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.security.auth.tenant.resolver.TenantResolverChain;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 租户过滤器。
 *
 * <p>从请求中解析租户信息并设置到 {@link TenantContextHolder}。
 * 继承 {@link OncePerRequestFilter} 确保每个请求只过滤一次。
 *
 * <p>处理流程：
 * <ol>
 *   <li>调用 {@link TenantResolverChain#resolve(HttpServletRequest)} 解析租户</li>
 *   <li>将租户 ID 设置到 {@link TenantContextHolder}</li>
 *   <li>执行后续过滤器链</li>
 *   <li>在 finally 块中清除租户上下文</li>
 * </ol>
 *
 * <p>异常处理：
 * <ul>
 *   <li>当 {@link TenantException} 发生时，返回 400 Bad Request JSON 响应</li>
 *   <li>JSON 响应格式：{@code {"code": 20003, "message": "无法解析租户信息"}}</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolverChain resolverChain;
    private final TenantContextHolder tenantContextHolder;
    private final ObjectMapper objectMapper;

    /**
     * 构造租户过滤器。
     *
     * @param resolverChain 租户解析器链（非空）
     * @param tenantContextHolder 租户上下文持有者（非空）
     */
    public TenantFilter(@NonNull TenantResolverChain resolverChain,
                        @NonNull TenantContextHolder tenantContextHolder) {
        this.resolverChain = resolverChain;
        this.tenantContextHolder = tenantContextHolder;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 构造租户过滤器（使用自定义 ObjectMapper）。
     *
     * @param resolverChain 租户解析器链（非空）
     * @param tenantContextHolder 租户上下文持有者（非空）
     * @param objectMapper JSON 序列化器（非空）
     */
    public TenantFilter(@NonNull TenantResolverChain resolverChain,
                        @NonNull TenantContextHolder tenantContextHolder,
                        @NonNull ObjectMapper objectMapper) {
        this.resolverChain = resolverChain;
        this.tenantContextHolder = tenantContextHolder;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 解析租户
            TenantContext tenantContext = resolverChain.resolve(request);

            if (tenantContext != null) {
                String tenantId = tenantContext.getTenantId();
                log.debug("设置租户上下文: tenantId={}", tenantId);
                tenantContextHolder.setTenantId(tenantId);
            } else {
                log.debug("未解析到租户信息");
            }

            // 继续执行过滤器链
            filterChain.doFilter(request, response);

        } catch (TenantException e) {
            log.warn("租户解析失败: {}", e.getMessage());
            handleTenantException(response, e);

        } finally {
            // 确保清除租户上下文
            tenantContextHolder.clear();
            log.debug("清除租户上下文");
        }
    }

    /**
     * 处理租户异常，返回 400 JSON 响应。
     *
     * @param response HTTP 响应
     * @param exception 租户异常
     * @throws IOException 写入响应时可能抛出
     */
    private void handleTenantException(HttpServletResponse response, TenantException exception)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");

        // 构建错误响应
        ErrorResponse errorResponse = new ErrorResponse(
                exception.getErrorCode().getCode(),
                exception.getMessage()
        );

        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        // 不过滤 Actuator 端点
        if (path.startsWith("/actuator/")) {
            return true;
        }

        // 不过滤静态资源
        if (path.startsWith("/static/") || path.startsWith("/public/")) {
            return true;
        }

        return false;
    }

    /**
     * 错误响应 DTO。
     */
    private record ErrorResponse(int code, String message) {
    }
}
