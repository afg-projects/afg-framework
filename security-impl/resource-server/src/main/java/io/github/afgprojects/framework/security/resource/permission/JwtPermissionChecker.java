package io.github.afgprojects.framework.security.resource.permission;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * 基于 JWT Token 的权限校验器。
 *
 * <p>从 JWT Token 中解析用户权限和角色信息，进行本地校验。
 * 无需访问数据库，适用于资源服务器场景。
 *
 * <h3>JWT Token Claims 约定</h3>
 * <ul>
 *   <li>permissions: 权限列表，如 ["user:read", "user:write"]</li>
 *   <li>roles: 角色列表，如 ["ADMIN", "USER"]</li>
 *   <li>tenant_id: 租户 ID</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Component
public class JwtPermissionChecker {

    private static final String PERMISSIONS_CLAIM = "permissions";
    private static final String ROLES_CLAIM = "roles";
    private static final String TENANT_ID_CLAIM = "tenant_id";

    /**
     * 检查当前用户是否具有指定权限。
     */
    public boolean hasPermission(@NonNull String permission) {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return false;
        }
        Set<String> permissions = getPermissions(jwt);
        return permissions.contains(permission);
    }

    /**
     * 检查当前用户是否具有指定角色。
     */
    public boolean hasRole(@NonNull String role) {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return false;
        }
        Set<String> roles = getRoles(jwt);
        return roles.contains(role);
    }

    /**
     * 检查当前用户是否具有任意指定权限。
     */
    public boolean hasAnyPermission(@NonNull Set<String> permissions) {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return false;
        }
        Set<String> userPermissions = getPermissions(jwt);
        return permissions.stream().anyMatch(userPermissions::contains);
    }

    /**
     * 检查当前用户是否具有任意指定角色。
     */
    public boolean hasAnyRole(@NonNull Set<String> roles) {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return false;
        }
        Set<String> userRoles = getRoles(jwt);
        return roles.stream().anyMatch(userRoles::contains);
    }

    /**
     * 获取当前用户的所有权限。
     */
    @NonNull
    public Set<String> getPermissions() {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? getPermissions(jwt) : Collections.emptySet();
    }

    /**
     * 获取当前用户的所有角色。
     */
    @NonNull
    public Set<String> getRoles() {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? getRoles(jwt) : Collections.emptySet();
    }

    /**
     * 获取当前用户的租户 ID。
     */
    @Nullable
    public String getTenantId() {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? jwt.getClaimAsString(TENANT_ID_CLAIM) : null;
    }

    /**
     * 获取当前用户的 ID。
     */
    @Nullable
    public String getUserId() {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? jwt.getSubject() : null;
    }

    @Nullable
    private Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof Jwt jwt ? jwt : null;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getPermissions(@NonNull Jwt jwt) {
        Object claim = jwt.getClaims().get(PERMISSIONS_CLAIM);
        if (claim instanceof Set<?> set) {
            return (Set<String>) set;
        }
        if (claim instanceof java.util.List<?> list) {
            return new java.util.HashSet<>((java.util.List<String>) list);
        }
        return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    private Set<String> getRoles(@NonNull Jwt jwt) {
        Object claim = jwt.getClaims().get(ROLES_CLAIM);
        if (claim instanceof Set<?> set) {
            return (Set<String>) set;
        }
        if (claim instanceof java.util.List<?> list) {
            return new java.util.HashSet<>((java.util.List<String>) list);
        }
        return Collections.emptySet();
    }
}