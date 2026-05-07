package io.github.afgprojects.framework.security.core.authorization;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * AFG 权限执行器接口。
 *
 * <p>提供统一的权限校验接口，支持基于角色和基于资源的权限控制。
 *
 * <p>实现类可基于 Casbin、Spring Security ACL 等权限框架。
 *
 * <p>示例用法：
 * <pre>{@code
 * // 检查角色
 * if (enforcer.hasRole(userId, "admin")) { ... }
 *
 * // 检查权限
 * if (enforcer.enforce(userId, "article", "read")) { ... }
 *
 * // 检查资源权限
 * if (enforcer.hasPermission(userId, "article:123", "delete")) { ... }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface AfgEnforcer {

    /**
     * 检查用户是否拥有指定角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     * @return 如果用户拥有该角色则返回 true
     */
    boolean hasRole(@NonNull String userId, @NonNull String role);

    /**
     * 检查用户是否拥有任意一个指定角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param roles 角色标识集合，永不为 null
     * @return 如果用户拥有任意一个角色则返回 true
     */
    boolean hasAnyRole(@NonNull String userId, @NonNull List<String> roles);

    /**
     * 检查用户是否拥有所有指定角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param roles 角色标识集合，永不为 null
     * @return 如果用户拥有所有角色则返回 true
     */
    boolean hasAllRoles(@NonNull String userId, @NonNull List<String> roles);

    /**
     * 执行权限检查（Casbin 风格）。
     *
     * <p>基于 subject、resource、action 三元组进行权限判断。
     *
     * @param subject 主体（通常是用户 ID），永不为 null
     * @param resource 资源（如 article、user、menu 等），永不为 null
     * @param action 操作（如 read、write、delete 等），永不为 null
     * @return 如果允许则返回 true
     */
    boolean enforce(@NonNull String subject, @NonNull String resource, @NonNull String action);

    /**
     * 执行带上下文的权限检查。
     *
     * <p>支持动态属性，如租户、组织等。
     *
     * @param subject 主体，永不为 null
     * @param resource 资源，永不为 null
     * @param action 操作，永不为 null
     * @param context 上下文属性，可为 null
     * @return 如果允许则返回 true
     */
    default boolean enforce(
            @NonNull String subject,
            @NonNull String resource,
            @NonNull String action,
            @Nullable Map<String, String> context) {
        return enforce(subject, resource, action);
    }

    /**
     * 检查用户是否拥有指定权限。
     *
     * @param userId 用户 ID，永不为 null
     * @param permission 权限标识（如 article:read、user:delete），永不为 null
     * @return 如果用户拥有该权限则返回 true
     */
    boolean hasPermission(@NonNull String userId, @NonNull String permission);

    /**
     * 检查用户是否拥有指定资源的指定操作权限。
     *
     * @param userId 用户 ID，永不为 null
     * @param resource 资源标识（如 article:123），永不为 null
     * @param action 操作标识，永不为 null
     * @return 如果用户拥有该权限则返回 true
     */
    default boolean hasPermission(@NonNull String userId, @NonNull String resource, @NonNull String action) {
        return enforce(userId, resource, action);
    }

    /**
     * 检查用户是否拥有任意一个指定权限。
     *
     * @param userId 用户 ID，永不为 null
     * @param permissions 权限标识集合，永不为 null
     * @return 如果用户拥有任意一个权限则返回 true
     */
    boolean hasAnyPermission(@NonNull String userId, @NonNull List<String> permissions);

    /**
     * 获取用户的所有角色。
     *
     * @param userId 用户 ID，永不为 null
     * @return 角色列表，永不为 null
     */
    @NonNull
    List<String> getRolesForUser(@NonNull String userId);

    /**
     * 获取用户的所有权限。
     *
     * @param userId 用户 ID，永不为 null
     * @return 权限列表，永不为 null
     */
    @NonNull
    List<String> getPermissionsForUser(@NonNull String userId);

    /**
     * 为用户添加角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     * @return 如果添加成功则返回 true
     */
    boolean addRoleForUser(@NonNull String userId, @NonNull String role);

    /**
     * 删除用户的角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     * @return 如果删除成功则返回 true
     */
    boolean deleteRoleForUser(@NonNull String userId, @NonNull String role);

    /**
     * 为角色添加权限。
     *
     * @param role 角色标识，永不为 null
     * @param permission 权限标识，永不为 null
     * @return 如果添加成功则返回 true
     */
    boolean addPermissionForRole(@NonNull String role, @NonNull String permission);

    /**
     * 删除角色的权限。
     *
     * @param role 角色标识，永不为 null
     * @param permission 权限标识，永不为 null
     * @return 如果删除成功则返回 true
     */
    boolean deletePermissionForRole(@NonNull String role, @NonNull String permission);

    /**
     * 为用户添加权限（直接授权）。
     *
     * @param userId 用户 ID，永不为 null
     * @param permission 权限标识，永不为 null
     * @return 如果添加成功则返回 true
     */
    default boolean addPermissionForUser(@NonNull String userId, @NonNull String permission) {
        return false;
    }

    /**
     * 删除用户的权限。
     *
     * @param userId 用户 ID，永不为 null
     * @param permission 权限标识，永不为 null
     * @return 如果删除成功则返回 true
     */
    default boolean deletePermissionForUser(@NonNull String userId, @NonNull String permission) {
        return false;
    }

    /**
     * 删除用户的所有角色。
     *
     * @param userId 用户 ID，永不为 null
     * @return 如果删除成功则返回 true
     */
    boolean deleteRolesForUser(@NonNull String userId);

    /**
     * 删除角色。
     *
     * @param role 角色标识，永不为 null
     * @return 如果删除成功则返回 true
     */
    boolean deleteRole(@NonNull String role);

    /**
     * 清除所有权限策略。
     *
     * <p>危险操作，仅用于测试或重置场景。
     */
    void clear();
}
