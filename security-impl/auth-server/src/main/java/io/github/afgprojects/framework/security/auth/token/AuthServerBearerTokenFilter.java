package io.github.afgprojects.framework.security.auth.token;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Auth Server Bearer Token 认证过滤器。
 *
 * <p>在请求处理前，从 HTTP 请求中提取 Bearer Token，
 * 使用 {@link AuthServerBearerTokenResolver} 验证 Token 并填充 SecurityContext。
 *
 * <p>此过滤器用于 auth-server 模块自身的端点认证，使需要认证的端点
 * （如 /auth-api/auth/user-info、/auth-api/auth/logout、
 * /auth-api/oauth2/authorize）可以在不依赖 resource-server 模块的情况下
 * 通过 Bearer Token 进行认证。
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>检查 SecurityContext 中是否已有认证信息，若有则跳过</li>
 *   <li>使用 AuthServerBearerTokenResolver 解析 Bearer Token</li>
 *   <li>将解析结果填充到 SecurityContext</li>
 *   <li>继续执行过滤器链</li>
 * </ol>
 *
 * @since 1.0.0
 */
@Slf4j
public class AuthServerBearerTokenFilter extends OncePerRequestFilter {

    private final AuthServerBearerTokenResolver tokenResolver;

    /**
     * 创建 Bearer Token 认证过滤器。
     *
     * @param tokenResolver Token 解析器
     */
    public AuthServerBearerTokenFilter(@NonNull AuthServerBearerTokenResolver tokenResolver) {
        this.tokenResolver = tokenResolver;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 如果 SecurityContext 中已有认证信息，跳过处理
        SecurityContext existingContext = SecurityContextHolder.getContext();
        if (existingContext.getAuthentication() != null && existingContext.getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 尝试从 Bearer Token 解析认证信息
        tokenResolver.resolve(request);

        filterChain.doFilter(request, response);
    }
}
