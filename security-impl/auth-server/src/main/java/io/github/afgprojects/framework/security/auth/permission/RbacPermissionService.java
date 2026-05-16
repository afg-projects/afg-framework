package io.github.afgprojects.framework.security.auth.permission;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.permission.PermissionService;
import io.github.afgprojects.framework.security.core.permission.RolePermissionStorage;

import java.util.Set;

/**
 * RBAC 权限服务实现。
 *
 * <p>基于角色的访问控制实现，通过 RolePermissionStorage 存储角色和权限关系。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * RolePermissionStorage storage = new JdbcRolePermissionStorage(dataManager);
 * RbacPermissionService permissionService = new RbacPermissionService(storage);
 *
 * // 检查角色
 * boolean isAdmin = permissionService.hasRole("user-001", "ADMIN");
 *
 * // 检查权限
 * boolean canRead = permissionService.hasPermission("user-001", "user:read");
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class RbacPermissionService implements PermissionService {

    private final RolePermissionStorage storage;

    /**
     * 构造函数。
     *
     * @param storage 角色权限存储
     */
    public RbacPermissionService(@NonNull RolePermissionStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean hasRole(@NonNull String userId, @NonNull String role) {
        return storage.hasRole(userId, role);
    }

    @Override
    public boolean hasAnyRole(@NonNull String userId, @NonNull Set<String> roles) {
        for (String role : roles) {
            if (hasRole(userId, role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAllRoles(@NonNull String userId, @NonNull Set<String> roles) {
        for (String role : roles) {
            if (!hasRole(userId, role)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasPermission(@NonNull String userId, @NonNull String permission) {
        // 获取用户的所有角色，检查是否有角色包含该权限
        Set<String> roles = getRoles(userId);
        for (String role : roles) {
            if (storage.hasPermission(role, permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyPermission(@NonNull String userId, @NonNull Set<String> permissions) {
        for (String permission : permissions) {
            if (hasPermission(userId, permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAllPermissions(@NonNull String userId, @NonNull Set<String> permissions) {
        for (String permission : permissions) {
            if (!hasPermission(userId, permission)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean check(@NonNull String userId, @NonNull String action, @Nullable String resourceId) {
        // RBAC 模型下，资源级别的权限检查等同于权限检查
        return hasPermission(userId, action);
    }

    @Override
    public boolean check(@NonNull String userId, @NonNull String action, @Nullable String resourceId, @Nullable String tenantId) {
        // RBAC 模型下，租户级别的权限检查等同于权限检查
        return hasPermission(userId, action);
    }

    @Override
    @NonNull
    public Set<String> getRoles(@NonNull String userId) {
        return storage.findRolesByUserId(userId);
    }

    @Override
    @NonNull
    public Set<String> getPermissions(@NonNull String userId) {
        return storage.findPermissionsByUserId(userId);
    }

    @Override
    public void grantRole(@NonNull String userId, @NonNull String role) {
        storage.grantRole(userId, role);
        log.info("Granted role to user: userId={}, role={}", userId, role);
    }

    @Override
    public void revokeRole(@NonNull String userId, @NonNull String role) {
        storage.revokeRole(userId, role);
        log.info("Revoked role from user: userId={}, role={}", userId, role);
    }

    @Override
    public void grantPermission(@NonNull String role, @NonNull String permission) {
        storage.grantPermission(role, permission);
        log.info("Granted permission to role: role={}, permission={}", role, permission);
    }

    @Override
    public void revokePermission(@NonNull String role, @NonNull String permission) {
        storage.revokePermission(role, permission);
        log.info("Revoked permission from role: role={}, permission={}", role, permission);
    }
}