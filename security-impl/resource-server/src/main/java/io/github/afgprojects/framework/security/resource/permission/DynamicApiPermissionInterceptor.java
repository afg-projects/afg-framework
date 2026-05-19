package io.github.afgprojects.framework.security.resource.permission;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 动态接口权限拦截器。
 *
 * <p>根据治理中心的配置动态校验接口权限。
 * 支持动态调整接口是否需要登录、是否需要权限。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicApiPermissionInterceptor implements HandlerInterceptor {

    private final DynamicApiPermissionManager permissionManager;
    private final CachedPermissionChecker permissionChecker;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        ApiPermissionConfig config = permissionManager.getConfig(path, method);

        // 没有配置，放行（由其他安全机制处理）
        if (config == null || !config.isEnabled()) {
            return true;
        }

        // 不需要登录，放行
        if (!config.isRequireAuth()) {
            return true;
        }

        // 检查登录状态
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("API requires auth but user not authenticated: {} {}", method, path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 不需要权限校验，放行
        if (!config.isRequirePermission()) {
            return true;
        }

        // 获取用户信息
        UserInfo userInfo = getUserInfo(authentication);
        if (userInfo == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 校验角色
        if (config.getRoles() != null && !config.getRoles().isEmpty()) {
            if (!checkRoles(config.getRoles(), config.getRoleLogical())) {
                log.debug("User lacks required roles: {} {}", method, path);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }

        // 校验权限
        if (config.getPermissions() != null && !config.getPermissions().isEmpty()) {
            if (!checkPermissions(config.getPermissions(), config.getPermissionLogical())) {
                log.debug("User lacks required permissions: {} {}", method, path);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }

        return true;
    }

    private boolean checkRoles(Set<String> requiredRoles, Logical logical) {
        if (logical == Logical.AND) {
            return requiredRoles.stream().allMatch(permissionChecker::hasRole);
        } else {
            return requiredRoles.stream().anyMatch(permissionChecker::hasRole);
        }
    }

    private boolean checkPermissions(Set<String> requiredPermissions, Logical logical) {
        if (logical == Logical.AND) {
            return requiredPermissions.stream().allMatch(permissionChecker::hasPermission);
        } else {
            return requiredPermissions.stream().anyMatch(permissionChecker::hasPermission);
        }
    }

    @Nullable
    private UserInfo getUserInfo(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String userId = jwt.getSubject();
            String tenantId = jwt.getClaimAsString("tenant_id");
            return new UserInfo(userId, tenantId);
        }
        return new UserInfo(authentication.getName(), null);
    }

    private record UserInfo(String userId, String tenantId) {}
}