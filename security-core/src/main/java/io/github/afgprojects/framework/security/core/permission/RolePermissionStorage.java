package io.github.afgprojects.framework.security.core.permission;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 角色权限存储接口。
 *
 * <p>SPI 接口，供业务实现持久化存储。
 *
 * @since 1.0.0
 */
public interface RolePermissionStorage {

    /**
     * 获取用户的所有角色。
     *
     * @param userId 用户 ID，永不为 null
     * @return 角色集合，永不为 null
     */
    @NonNull
    Set<String> findRolesByUserId(@NonNull String userId);

    /**
     * 获取角色的所有权限。
     *
     * @param role 角色标识，永不为 null
     * @return 权限集合，永不为 null
     */
    @NonNull
    Set<String> findPermissionsByRole(@NonNull String role);

    /**
     * 获取用户的所有权限（通过角色关联）。
     *
     * @param userId 用户 ID，永不为 null
     * @return 权限集合，永不为 null
     */
    @NonNull
    Set<String> findPermissionsByUserId(@NonNull String userId);

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

    /**
     * 检查用户是否具有角色。
     *
     * @param userId 用户 ID，永不为 null
     * @param role 角色标识，永不为 null
     * @return 如果具有角色返回 true
     */
    boolean hasRole(@NonNull String userId, @NonNull String role);

    /**
     * 检查角色是否具有权限。
     *
     * @param role 角色标识，永不为 null
     * @param permission 权限标识，永不为 null
     * @return 如果具有权限返回 true
     */
    boolean hasPermission(@NonNull String role, @NonNull String permission);
}