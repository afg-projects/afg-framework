package io.github.afgprojects.framework.security.core.permission;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 权限服务接口。
 *
 * <p>提供统一的权限检查接口，支持 RBAC 和 ABAC 模型。
 *
 * <h3>RBAC（基于角色的访问控制）</h3>
 * <p>用户 -> 角色 -> 权限
 * <pre>{@code
 * // 检查用户是否具有角色
 * boolean hasRole = permissionService.hasRole(userId, "ADMIN");
 *
 * // 检查用户是否具有权限
 * boolean hasPermission = permissionService.hasPermission(userId, "user:read");
 * }</pre>
 *
 * <h3>ABAC（基于属性的访问控制）</h3>
 * <p>主体属性 + 资源属性 + 操作 + 环境 -> 决策
 * <pre>{@code
 * // 检查用户是否可以对资源执行操作
 * boolean allowed = permissionService.check(userId, "user:delete", "user-001");
 * }</pre>
 *
 * @since 1.0.0
 */
public interface PermissionService {

    /**
     * 检查用户是否具有指定角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     * @return 如果具有角色返回 true
     */
    boolean hasRole(@NonNull String userId, @NonNull String role);

    /**
     * 检查用户是否具有任意指定角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param roles 角色集合，永不为 null
     * @return 如果具有任意角色返回 true
     */
    boolean hasAnyRole(@NonNull String userId, @NonNull Set<String> roles);

    /**
     * 检查用户是否具有所有指定角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param roles 角色集合，永不为 null
     * @return 如果具有所有角色返回 true
     */
    boolean hasAllRoles(@NonNull String userId, @NonNull Set<String> roles);

    /**
     * 检查用户是否具有指定权限。
     *
     * @param userId 用户 ID，永不为 null
     * @param permission 权限标识，永不为 null
     * @return 如果具有权限返回 true
     */
    boolean hasPermission(@NonNull String userId, @NonNull String permission);

    /**
     * 检查用户是否具有任意指定权限。
     *
     * @param userId 用户 ID，永不为 null
     * @param permissions 权限集合，永不为 null
     * @return 如果具有任意权限返回 true
     */
    boolean hasAnyPermission(@NonNull String userId, @NonNull Set<String> permissions);

    /**
     * 检查用户是否具有所有指定权限。
     *
     * @param userId 用户 ID，永不为 null
     * @param permissions 权限集合，永不为 null
     * @return 如果具有所有权限返回 true
     */
    boolean hasAllPermissions(@NonNull String userId, @NonNull Set<String> permissions);

    /**
     * 检查用户是否可以对资源执行操作（ABAC）。
     *
     * @param userId 用户 ID，永不为 null
     * @param action 操作（如 user:read, user:delete），永不为 null
     * @param resourceId 资源 ID
     * @return 如果允许返回 true
     */
    boolean check(@NonNull String userId, @NonNull String action, @Nullable String resourceId);

    /**
     * 检查用户是否可以对资源执行操作（带租户上下文）。
     *
     * @param userId 用户 ID，永不为 null
     * @param action 操作，永不为 null
     * @param resourceId 资源 ID
     * @param tenantId 租户 ID
     * @return 如果允许返回 true
     */
    boolean check(@NonNull String userId, @NonNull String action, @Nullable String resourceId, @Nullable String tenantId);

    /**
     * 获取用户的所有角色。
     *
     * @param userId 用户 ID，永不为 null
     * @return 角色集合，永不为 null
     */
    @NonNull
    Set<String> getRoles(@NonNull String userId);

    /**
     * 获取用户的所有权限。
     *
     * @param userId 用户 ID，永不为 null
     * @return 权限集合，永不为 null
     */
    @NonNull
    Set<String> getPermissions(@NonNull String userId);

    /**
     * 授予用户角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     */
    void grantRole(@NonNull String userId, @NonNull String role);

    /**
     * 撤销用户角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     */
    void revokeRole(@NonNull String userId, @NonNull String role);

    /**
     * 授予角色权限。
     *
     * @param role 角色标识，永不为 null
     * @param permission 权限标识，永不为 null
     */
    void grantPermission(@NonNull String role, @NonNull String permission);

    /**
     * 撤销角色权限。
     *
     * @param role 角色标识，永不为 null
     * @param permission 权限标识，永不为 null
     */
    void revokePermission(@NonNull String role, @NonNull String permission);
}