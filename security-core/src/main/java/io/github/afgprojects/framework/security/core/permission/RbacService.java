package io.github.afgprojects.framework.security.core.permission;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 角色权限服务接口。
 *
 * <p>提供基于角色的访问控制（RBAC）服务。
 *
 * <h3>RBAC 模型</h3>
 * <p>用户 -> 角色 -> 权限
 * <pre>{@code
 * // 检查用户是否具有角色
 * boolean hasRole = rbacService.hasRole(userId, "ADMIN", tenantId);
 *
 * // 检查用户是否具有权限
 * boolean hasPermission = rbacService.hasPermission(userId, "user:read", tenantId);
 *
 * // 获取用户所有角色
 * Set<String> roles = rbacService.getRoles(userId, tenantId);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface RbacService {

    /**
     * 获取用户的所有角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     * @return 角色集合，永不为 null
     */
    @NonNull
    Set<String> getRoles(@NonNull String userId, @Nullable String tenantId);

    /**
     * 获取用户的所有权限。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     * @return 权限集合，永不为 null
     */
    @NonNull
    Set<String> getPermissions(@NonNull String userId, @Nullable String tenantId);

    /**
     * 检查用户是否具有指定角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     * @return 如果具有角色返回 true
     */
    boolean hasRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId);

    /**
     * 检查用户是否具有指定权限。
     *
     * @param userId 用户 ID，永不为 null
     * @param permission 权限标识，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     * @return 如果具有权限返回 true
     */
    boolean hasPermission(@NonNull String userId, @NonNull String permission, @Nullable String tenantId);

    /**
     * 授予用户角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     */
    void grantRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId);

    /**
     * 撤销用户角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     */
    void revokeRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId);

    /**
     * 授予角色权限。
     *
     * @param role 角色标识，永不为 null
     * @param permission 权限标识，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     */
    void grantPermission(@NonNull String role, @NonNull String permission, @Nullable String tenantId);

    /**
     * 撤销角色权限。
     *
     * @param role 角色标识，永不为 null
     * @param permission 权限标识，永不为 null
     * @param tenantId 租户 ID，可为 null（非多租户场景）
     */
    void revokePermission(@NonNull String role, @NonNull String permission, @Nullable String tenantId);
}
