package io.github.afgprojects.framework.security.resource.permission;

import io.github.afgprojects.framework.security.core.token.JwtClaimsExtractor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 基于 JWT Token 的权限校验器。
 *
 * <p>从 JWT Token 中解析用户权限和角色信息，进行本地校验。
 * 无需访问数据库，适用于资源服务器场景。
 *
 * <p>使用 {@link JwtClaimsExtractor} 从 JWT Claims 中提取权限和角色信息，
 * Claim 名称映射由 {@link io.github.afgprojects.framework.security.core.token.JwtClaimsConfig} 统一管理。
 *
 * @since 1.0.0
 */
@Component
public class JwtPermissionChecker {

    private final JwtClaimsExtractor claimsExtractor;

    /**
     * 使用默认配置创建权限校验器。
     */
    public JwtPermissionChecker() {
        this.claimsExtractor = new JwtClaimsExtractor();
    }

    /**
     * 使用指定 Claims 提取器创建权限校验器。
     *
     * @param claimsExtractor JWT Claims 提取器
     */
    public JwtPermissionChecker(@NonNull JwtClaimsExtractor claimsExtractor) {
        this.claimsExtractor = claimsExtractor;
    }

    /**
     * 检查当前用户是否具有指定权限。
     */
    public boolean hasPermission(@NonNull String permission) {
        Map<String, Object> claims = getCurrentClaims();
        if (claims == null) {
            return false;
        }
        Set<String> permissions = claimsExtractor.extractPermissions(claims);
        return permissions.contains(permission);
    }

    /**
     * 检查当前用户是否具有指定角色。
     */
    public boolean hasRole(@NonNull String role) {
        Map<String, Object> claims = getCurrentClaims();
        if (claims == null) {
            return false;
        }
        Set<String> roles = claimsExtractor.extractRoles(claims);
        return roles.contains(role);
    }

    /**
     * 检查当前用户是否具有任意指定权限。
     */
    public boolean hasAnyPermission(@NonNull Set<String> permissions) {
        Map<String, Object> claims = getCurrentClaims();
        if (claims == null) {
            return false;
        }
        Set<String> userPermissions = claimsExtractor.extractPermissions(claims);
        return permissions.stream().anyMatch(userPermissions::contains);
    }

    /**
     * 检查当前用户是否具有任意指定角色。
     */
    public boolean hasAnyRole(@NonNull Set<String> roles) {
        Map<String, Object> claims = getCurrentClaims();
        if (claims == null) {
            return false;
        }
        Set<String> userRoles = claimsExtractor.extractRoles(claims);
        return roles.stream().anyMatch(userRoles::contains);
    }

    /**
     * 获取当前用户的所有权限。
     */
    @NonNull
    public Set<String> getPermissions() {
        Map<String, Object> claims = getCurrentClaims();
        return claims != null ? claimsExtractor.extractPermissions(claims) : Collections.emptySet();
    }

    /**
     * 获取当前用户的所有角色。
     */
    @NonNull
    public Set<String> getRoles() {
        Map<String, Object> claims = getCurrentClaims();
        return claims != null ? claimsExtractor.extractRoles(claims) : Collections.emptySet();
    }

    /**
     * 获取当前用户的租户 ID。
     */
    @Nullable
    public String getTenantId() {
        Map<String, Object> claims = getCurrentClaims();
        return claims != null ? claimsExtractor.extractTenantId(claims) : null;
    }

    /**
     * 获取当前用户的 ID。
     */
    @Nullable
    public String getUserId() {
        Map<String, Object> claims = getCurrentClaims();
        return claims != null ? claimsExtractor.extractUserId(claims) : null;
    }

    @Nullable
    private Map<String, Object> getCurrentClaims() {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? jwt.getClaims() : null;
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
}