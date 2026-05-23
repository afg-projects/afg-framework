package io.github.afgprojects.framework.security.auth.permission;

import io.github.afgprojects.framework.core.security.datascope.DataScopeContext;
import io.github.afgprojects.framework.core.security.datascope.DataScopeContextHolder;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;
import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.permission.DataScopeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 数据权限拦截器。
 *
 * <p>从 Spring Security 上下文获取已认证用户信息，通过 DataScopeService
 * 获取用户的数据权限配置，并设置到 DataScopeContextHolder 中。
 *
 * <p>请求结束后自动清除上下文。
 *
 * <h3>安全说明</h3>
 * <p>只从 SecurityContext 获取用户信息，不接受请求头传递，
 * 防止越权攻击。
 *
 * @since 1.0.0
 */
@Slf4j
public class DataScopeInterceptor extends OncePerRequestFilter {

    private final DataScopeService dataScopeService;
    private final AuthSecurityProperties.PermissionConfig permissionConfig;

    public DataScopeInterceptor(DataScopeService dataScopeService, AuthSecurityProperties.PermissionConfig permissionConfig) {
        this.dataScopeService = dataScopeService;
        this.permissionConfig = permissionConfig;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 从 SecurityContext 获取已认证用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (!isAuthenticatedUser(auth)) {
                log.debug("No authenticated user, skipping data scope initialization");
                filterChain.doFilter(request, response);
                return;
            }

            // 提取 AfgAuthentication
            AfgAuthentication afgAuth = extractAfgAuthentication(auth);
            if (afgAuth == null) {
                log.debug("Authentication is not AfgAuthentication type: {}", auth.getClass().getSimpleName());
                filterChain.doFilter(request, response);
                return;
            }

            // 获取用户信息
            AfgUserDetails userDetails = afgAuth.getUserDetails();
            String userId = userDetails.getUserId();
            String tenantId = userDetails.getTenantId();

            // 获取数据权限配置
            DataScope dataScope = dataScopeService.getDataScope(userId, tenantId);

            // 构建数据权限上下文
            DataScopeContext context = buildContext(userId, tenantId, dataScope, afgAuth.isAdmin());

            // 设置上下文
            DataScopeContextHolder.setContext(context);

            log.debug("Data scope context initialized: userId={}, tenantId={}, scopeType={}, isAdmin={}",
                    userId, tenantId, dataScope.scopeType(), afgAuth.isAdmin());

            filterChain.doFilter(request, response);
        } finally {
            // 清除上下文
            DataScopeContextHolder.clear();
            log.debug("Data scope context cleared");
        }
    }

    /**
     * 判断是否为已认证用户
     */
    private boolean isAuthenticatedUser(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    /**
     * 从 Authentication 提取 AfgAuthentication
     */
    private AfgAuthentication extractAfgAuthentication(Authentication auth) {
        if (auth instanceof AfgAuthentication) {
            return (AfgAuthentication) auth;
        }
        if (auth.getPrincipal() instanceof AfgAuthentication) {
            return (AfgAuthentication) auth.getPrincipal();
        }
        return null;
    }

    /**
     * 构建数据权限上下文
     */
    private DataScopeContext buildContext(
            String userId,
            String tenantId,
            DataScope dataScope,
            boolean isAdmin) {

        DataScopeContext.DataScopeContextBuilder builder = DataScopeContext.builder()
                .userId(Long.parseLong(userId));

        // 管理员拥有全部数据权限
        if (isAdmin) {
            return builder.allDataPermission(true).build();
        }

        // 根据数据范围类型设置权限
        switch (dataScope.scopeType()) {
            case ALL:
                builder.allDataPermission(true);
                break;

            case SELF:
                builder.allDataPermission(false);
                break;

            case DEPT:
            case DEPT_AND_CHILD:
                builder.allDataPermission(false);
                // 从服务获取可访问部门 ID
                Set<Long> accessibleDeptIds = dataScopeService.getAccessibleDeptIds(userId, tenantId);
                if (accessibleDeptIds != null && !accessibleDeptIds.isEmpty()) {
                    builder.accessibleDeptIds(accessibleDeptIds);
                }
                break;

            case CUSTOM:
                builder.allDataPermission(false);
                builder.customCondition(dataScope.customCondition());
                break;

            default:
                // 默认使用配置的默认数据范围
                builder.allDataPermission("ALL".equals(permissionConfig.getDefaultDataScope()));
        }

        return builder.build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 如果未启用数据权限拦截器，跳过过滤
        return !permissionConfig.isEnabled() || !permissionConfig.isDataScopeInterceptorEnabled();
    }
}
