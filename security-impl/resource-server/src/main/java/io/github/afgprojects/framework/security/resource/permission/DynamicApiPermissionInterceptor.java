package io.github.afgprojects.framework.security.resource.permission;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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
 *
 * <p>校验通过后，会设置请求属性标记权限已校验，
 * 后续的注解式权限校验（@RequirePermission/@RequireRole）可以跳过已校验的请求。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicApiPermissionInterceptor implements HandlerInterceptor {

    /**
     * 请求属性：标记动态权限校验已通过。
     * 后续 PermissionAspect 检查此属性，如果为 true 则跳过校验。
     */
    public static final String PERMISSION_CHECKED_ATTR = "afg.security.permission.checked";

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

        // 不需要登录，标记已校验并放行
        if (!config.isRequireAuth()) {
            request.setAttribute(PERMISSION_CHECKED_ATTR, true);
            return true;
        }

        // 检查登录状态
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("API requires auth but user not authenticated: {} {}", method, path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 不需要权限校验，标记已校验并放行
        if (!config.isRequirePermission()) {
            request.setAttribute(PERMISSION_CHECKED_ATTR, true);
            return true;
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

        // 校验通过，设置标记，后续注解校验可跳过
        request.setAttribute(PERMISSION_CHECKED_ATTR, true);
        log.debug("Dynamic permission check passed: {} {}", method, path);

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
}