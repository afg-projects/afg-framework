package io.github.afgprojects.framework.security.auth.tenant.filter;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import io.github.afgprojects.framework.security.core.tenant.TenantResolverChain;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户过滤器。
 *
 * <p>在每个请求中解析租户上下文并设置到 {@link TenantContextHolder}。
 * 请求处理完成后自动清除租户上下文。
 *
 * @since 1.0.0
 */
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolverChain resolverChain;
    private final TenantContextHolder tenantContextHolder;

    /**
     * 构造租户过滤器。
     *
     * @param resolverChain      租户解析器链
     * @param tenantContextHolder 租户上下文持有者
     */
    public TenantFilter(@NonNull TenantResolverChain resolverChain,
                        @NonNull TenantContextHolder tenantContextHolder) {
        this.resolverChain = resolverChain;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            TenantContext tenantContext = resolverChain.resolve(request);

            if (tenantContext != null) {
                tenantContextHolder.setTenantId(tenantContext.getTenantId());
                log.debug("设置租户上下文: tenantId={}", tenantContext.getTenantId());
            }

            filterChain.doFilter(request, response);
        } catch (TenantException e) {
            log.error("租户解析失败: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } finally {
            tenantContextHolder.clear();
            log.debug("清除租户上下文");
        }
    }
}
