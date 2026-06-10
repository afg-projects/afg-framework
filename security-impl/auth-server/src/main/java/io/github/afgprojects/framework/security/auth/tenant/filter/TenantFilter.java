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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 租户过滤器。
 *
 * <p>在每个请求中解析租户上下文并设置到 {@link TenantContextHolder}。
 * 请求处理完成后自动清除租户上下文。
 *
 * <p>对于公开端点（登录、验证码、session 等），跳过租户检查。
 *
 * @since 1.0.0
 */
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolverChain resolverChain;
    private final TenantContextHolder tenantContextHolder;

    /**
     * 不需要租户检查的公开端点路径模式。
     */
    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/auth-api/auth/login",
            "/auth-api/auth/refresh",
            "/auth-api/auth/session",
            "/auth-api/auth/captcha",
            "/auth-api/auth/captcha/**",
            "/auth-api/oauth2/token",
            "/auth-api/oauth2/introspect",
            "/auth-api/oauth2/revoke",
            "/.well-known/**",
            "/actuator/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

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

        String requestPath = request.getRequestURI();

        // 公开端点跳过租户检查
        if (shouldSkip(requestPath)) {
            log.debug("跳过租户检查: path={}", requestPath);
            try {
                filterChain.doFilter(request, response);
            } finally {
                tenantContextHolder.clear();
            }
            return;
        }

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

    /**
     * 判断请求路径是否应跳过租户检查。
     *
     * @param requestPath 请求路径
     * @return 是否跳过
     */
    private boolean shouldSkip(String requestPath) {
        for (String pattern : SKIP_PATHS) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }
}
