package io.github.afgprojects.framework.security.core.authentication;

import java.util.Collection;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * AFG 认证令牌接口。
 *
 * <p>扩展 Spring Security 的 Authentication 接口，提供用户详情、租户信息等扩展能力。
 *
 * <p>实现类应包含完整的认证信息，包括用户主体、权限、租户等。
 *
 * @since 1.0.0
 */
public interface AfgAuthentication extends Authentication {

    /**
     * 获取用户详情。
     *
     * @return 用户详情，永不为 null
     */
    @NonNull
    AfgUserDetails getUserDetails();

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID，永不为 null
     */
    @NonNull
    default String getUserId() {
        return getUserDetails().getUserId();
    }

    /**
     * 获取用户名。
     *
     * @return 用户名，永不为 null
     */
    @Override
    default @NonNull String getName() {
        return getUserDetails().getUsername();
    }

    /**
     * 获取用户凭证（密码等）。
     *
     * @return 凭证，认证完成后通常为 null
     */
    @Override
    @Nullable
    Object getCredentials();

    /**
     * 获取用户权限集合。
     *
     * @return 权限集合，永不为 null
     */
    @Override
    @NonNull
    Collection<? extends GrantedAuthority> getAuthorities();

    /**
     * 获取用户详情对象。
     *
     * @return 用户详情
     */
    @Override
    default @Nullable Object getPrincipal() {
        return getUserDetails();
    }

    /**
     * 获取租户 ID。
     *
     * @return 租户 ID，单租户场景返回 null
     */
    @Nullable
    default String getTenantId() {
        return getUserDetails().getTenantId();
    }

    /**
     * 获取组织 ID。
     *
     * @return 组织 ID
     */
    @Nullable
    default String getOrganizationId() {
        return getUserDetails().getOrganizationId();
    }

    /**
     * 获取用户角色集合。
     *
     * @return 角色集合，永不为 null
     */
    default java.util.@NonNull Set<String> getRoles() {
        return getUserDetails().getRoles();
    }

    /**
     * 判断是否已认证。
     *
     * @return 默认返回 true
     */
    @Override
    default boolean isAuthenticated() {
        return true;
    }

    /**
     * 设置认证状态。
     *
     * @param isAuthenticated 认证状态
     * @throws IllegalArgumentException 如果参数非法
     */
    @Override
    default void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        // 默认实现为空，由具体实现类决定是否支持
    }

    /**
     * 判断是否为管理员。
     *
     * @return 如果是管理员则返回 true
     */
    default boolean isAdmin() {
        return getUserDetails().isAdmin();
    }
}
