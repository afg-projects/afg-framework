package io.github.afgprojects.framework.security.core.authorization;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;

/**
 * AFG 安全上下文接口。
 *
 * <p>提供获取当前认证用户信息的能力，类似 Spring Security 的 SecurityContextHolder。
 *
 * <p>实现类可基于 ThreadLocal、Reactor Context 或其他机制存储安全上下文。
 *
 * <p>示例用法：
 * <pre>{@code
 * // 获取当前用户
 * AfgUserDetails user = securityContext.getCurrentUser();
 *
 * // 获取当前用户 ID
 * String userId = securityContext.getCurrentUserId();
 *
 * // 判断是否已登录
 * if (securityContext.isAuthenticated()) { ... }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface AfgSecurityContext {

    /**
     * 获取当前认证令牌。
     *
     * @return 认证令牌，未认证时返回 null
     */
    @Nullable
    AfgAuthentication getAuthentication();

    /**
     * 设置当前认证令牌。
     *
     * @param authentication 认证令牌，可为 null（表示清除认证）
     */
    void setAuthentication(@Nullable AfgAuthentication authentication);

    /**
     * 判断当前是否已认证。
     *
     * @return 如果已认证则返回 true
     */
    default boolean isAuthenticated() {
        AfgAuthentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    /**
     * 获取当前用户详情。
     *
     * @return 用户详情，未认证时返回 null
     */
    @Nullable
    default AfgUserDetails getCurrentUser() {
        AfgAuthentication auth = getAuthentication();
        return auth != null ? auth.getUserDetails() : null;
    }

    /**
     * 获取当前用户详情，抛出异常如果未认证。
     *
     * @return 用户详情，永不为 null
     * @throws IllegalStateException 如果未认证
     */
    @NonNull
    default AfgUserDetails getRequiredCurrentUser() {
        AfgUserDetails user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return user;
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 用户 ID，未认证时返回 null
     */
    @Nullable
    default String getCurrentUserId() {
        AfgUserDetails user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户 ID，抛出异常如果未认证。
     *
     * @return 用户 ID，永不为 null
     * @throws IllegalStateException 如果未认证
     */
    @NonNull
    default String getRequiredCurrentUserId() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return userId;
    }

    /**
     * 获取当前用户名。
     *
     * @return 用户名，未认证时返回 null
     */
    @Nullable
    default String getCurrentUsername() {
        AfgUserDetails user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前租户 ID。
     *
     * @return 租户 ID，未认证或单租户场景返回 null
     */
    @Nullable
    default String getCurrentTenantId() {
        AfgAuthentication auth = getAuthentication();
        return auth != null ? auth.getTenantId() : null;
    }

    /**
     * 获取当前组织 ID。
     *
     * @return 组织 ID
     */
    @Nullable
    default String getCurrentOrganizationId() {
        AfgAuthentication auth = getAuthentication();
        return auth != null ? auth.getOrganizationId() : null;
    }

    /**
     * 判断当前用户是否为管理员。
     *
     * @return 如果是管理员则返回 true，未认证时返回 false
     */
    default boolean isAdmin() {
        AfgAuthentication auth = getAuthentication();
        return auth != null && auth.isAdmin();
    }

    /**
     * 判断当前用户是否拥有指定角色。
     *
     * @param role 角色标识，永不为 null
     * @return 如果拥有该角色则返回 true，未认证时返回 false
     */
    default boolean hasRole(@NonNull String role) {
        AfgUserDetails user = getCurrentUser();
        return user != null && user.getRoles().contains(role);
    }

    /**
     * 清除当前安全上下文。
     *
     * <p>通常在用户登出或请求结束时调用。
     */
    default void clear() {
        setAuthentication(null);
    }
}
