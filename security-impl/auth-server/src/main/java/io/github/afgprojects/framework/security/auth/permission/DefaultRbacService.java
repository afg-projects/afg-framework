package io.github.afgprojects.framework.security.auth.permission;

import io.github.afgprojects.framework.security.core.permission.RbacService;
import io.github.afgprojects.framework.security.core.permission.RolePermissionStorage;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * 默认 RBAC 服务实现。
 *
 * <p>基于角色的访问控制实现，通过 {@link RolePermissionStorage} 存储角色和权限关系。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * RolePermissionStorage storage = new JdbcRolePermissionStorage(dataManager);
 * RbacService rbacService = new DefaultRbacService(storage);
 *
 * // 检查角色
 * boolean isAdmin = rbacService.hasRole("user-001", "ADMIN", "tenant-001");
 *
 * // 检查权限
 * boolean canRead = rbacService.hasPermission("user-001", "user:read", "tenant-001");
 *
 * // 获取用户所有角色
 * Set<String> roles = rbacService.getRoles("user-001", "tenant-001");
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class DefaultRbacService implements RbacService {

    private final RolePermissionStorage storage;

    /**
     * 构造函数。
     *
     * @param storage 角色权限存储
     */
    public DefaultRbacService(@NonNull RolePermissionStorage storage) {
        this.storage = storage;
    }

    @Override
    @NonNull
    public Set<String> getRoles(@NonNull String userId, @Nullable String tenantId) {
        // 当前 RolePermissionStorage 不支持租户隔离，忽略 tenantId
        if (tenantId != null) {
            log.debug("Tenant isolation not supported by storage, tenantId={} will be ignored", tenantId);
        }
        return storage.findRolesByUserId(userId);
    }

    @Override
    @NonNull
    public Set<String> getPermissions(@NonNull String userId, @Nullable String tenantId) {
        // 当前 RolePermissionStorage 不支持租户隔离，忽略 tenantId
        if (tenantId != null) {
            log.debug("Tenant isolation not supported by storage, tenantId={} will be ignored", tenantId);
        }
        Set<String> roles = getRoles(userId, tenantId);
        Set<String> permissions = new HashSet<>();
        for (String role : roles) {
            permissions.addAll(storage.findPermissionsByRole(role));
        }
        return permissions;
    }

    @Override
    public boolean hasRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId) {
        return getRoles(userId, tenantId).contains(role);
    }

    @Override
    public boolean hasPermission(@NonNull String userId, @NonNull String permission, @Nullable String tenantId) {
        return getPermissions(userId, tenantId).contains(permission);
    }

    @Override
    public void grantRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId) {
        // 当前 RolePermissionStorage 不支持租户隔离，忽略 tenantId
        if (tenantId != null) {
            log.debug("Tenant isolation not supported by storage, tenantId={} will be ignored", tenantId);
        }
        storage.grantRole(userId, role);
        log.info("Granted role to user: userId={}, role={}", userId, role);
    }

    @Override
    public void revokeRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId) {
        // 当前 RolePermissionStorage 不支持租户隔离，忽略 tenantId
        if (tenantId != null) {
            log.debug("Tenant isolation not supported by storage, tenantId={} will be ignored", tenantId);
        }
        storage.revokeRole(userId, role);
        log.info("Revoked role from user: userId={}, role={}", userId, role);
    }

    @Override
    public void grantPermission(@NonNull String role, @NonNull String permission, @Nullable String tenantId) {
        // 当前 RolePermissionStorage 不支持租户隔离，忽略 tenantId
        if (tenantId != null) {
            log.debug("Tenant isolation not supported by storage, tenantId={} will be ignored", tenantId);
        }
        storage.grantPermission(role, permission);
        log.info("Granted permission to role: role={}, permission={}", role, permission);
    }

    @Override
    public void revokePermission(@NonNull String role, @NonNull String permission, @Nullable String tenantId) {
        // 当前 RolePermissionStorage 不支持租户隔离，忽略 tenantId
        if (tenantId != null) {
            log.debug("Tenant isolation not supported by storage, tenantId={} will be ignored", tenantId);
        }
        storage.revokePermission(role, permission);
        log.info("Revoked permission from role: role={}, permission={}", role, permission);
    }
}
