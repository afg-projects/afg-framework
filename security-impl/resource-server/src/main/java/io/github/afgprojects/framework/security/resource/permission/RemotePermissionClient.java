package io.github.afgprojects.framework.security.resource.permission;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 远程权限校验客户端接口。
 *
 * <p>资源服务器通过此接口调用认证服务器进行权限校验。
 * 实现类可以使用 HTTP、gRPC 等方式调用认证服务器。
 *
 * @since 1.0.0
 */
public interface RemotePermissionClient {

    /**
     * 检查用户是否具有指定权限。
     *
     * @param userId 用户 ID
     * @param permission 权限标识
     * @param tenantId 租户 ID（可选）
     * @return 如果具有权限返回 true
     */
    boolean hasPermission(@NonNull String userId, @NonNull String permission, @Nullable String tenantId);

    /**
     * 检查用户是否具有指定角色。
     *
     * @param userId 用户 ID
     * @param role 角色标识
     * @param tenantId 租户 ID（可选）
     * @return 如果具有角色返回 true
     */
    boolean hasRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId);

    /**
     * 获取用户的所有权限。
     *
     * @param userId 用户 ID
     * @param tenantId 租户 ID（可选）
     * @return 权限集合
     */
    java.util.Set<String> getPermissions(@NonNull String userId, @Nullable String tenantId);
}