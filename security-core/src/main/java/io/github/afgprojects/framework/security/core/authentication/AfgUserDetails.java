package io.github.afgprojects.framework.security.core.authentication;

import java.util.Collection;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

/**
 * AFG 用户主体接口。
 *
 * <p>扩展 Spring Security 的 UserDetails 接口，提供多租户、组织架构等企业级特性。
 *
 * <p>实现类应包含用户的基本信息、权限信息和租户信息等。
 *
 * @since 1.0.0
 */
public interface AfgUserDetails extends org.springframework.security.core.userdetails.UserDetails {

    /**
     * 获取用户唯一标识。
     *
     * @return 用户 ID，永不为 null
     */
    @NonNull
    String getUserId();

    /**
     * 获取用户名（登录名）。
     *
     * @return 用户名，永不为 null
     */
    @Override
    @NonNull
    String getUsername();

    /**
     * 获取用户显示名称。
     *
     * @return 显示名称，如为 null 则返回用户名
     */
    @Nullable
    default String getDisplayName() {
        return getUsername();
    }

    /**
     * 获取用户所属租户 ID。
     *
     * @return 租户 ID，单租户场景返回 null
     */
    @Nullable
    String getTenantId();

    /**
     * 获取用户所属组织 ID。
     *
     * @return 组织 ID，如无组织则返回 null
     */
    @Nullable
    default String getOrganizationId() {
        return null;
    }

    /**
     * 获取用户角色集合。
     *
     * <p>角色是权限的分组，一个角色包含多个权限。
     *
     * @return 角色集合，永不为 null
     */
    @NonNull
    Set<String> getRoles();

    /**
     * 获取用户权限集合。
     *
     * <p>权限用于细粒度的访问控制。
     *
     * @return 权限集合，永不为 null
     */
    @Override
    @NonNull
    Collection<? extends GrantedAuthority> getAuthorities();

    /**
     * 判断用户是否为管理员。
     *
     * @return 如果是管理员则返回 true
     */
    default boolean isAdmin() {
        return false;
    }

    /**
     * 获取用户类型。
     *
     * @return 用户类型，如 user、admin、system 等
     */
    @Nullable
    default String getUserType() {
        return "user";
    }

    /**
     * 获取用户登录终端类型。
     *
     * @return 终端类型，如 web、app、api 等
     */
    @Nullable
    default String getClientType() {
        return null;
    }

    /**
     * 获取用户登录 IP 地址。
     *
     * @return IP 地址
     */
    @Nullable
    default String getLoginIp() {
        return null;
    }

    /**
     * 判断用户账号是否未过期。
     *
     * @return 默认返回 true
     */
    @Override
    default boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 判断用户账号是否未锁定。
     *
     * @return 默认返回 true
     */
    @Override
    default boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 判断用户凭证是否未过期。
     *
     * @return 默认返回 true
     */
    @Override
    default boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 判断用户是否启用。
     *
     * @return 默认返回 true
     */
    @Override
    default boolean isEnabled() {
        return true;
    }
}
