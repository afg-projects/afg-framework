package io.github.afgprojects.framework.security.auth.permission;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import io.github.afgprojects.framework.security.core.permission.RolePermissionStorage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存角色权限存储实现。
 *
 * <p>适用于单机部署和测试环境，使用 ConcurrentHashMap 存储。
 * 生产环境应使用 JDBC 或其他持久化存储实现。
 *
 * @since 1.0.0
 */
@Slf4j
public class InMemoryRolePermissionStorage implements RolePermissionStorage {

    /**
     * 用户 -> 角色集合
     */
    private final Map<String, Set<String>> userRoles = new ConcurrentHashMap<>();

    /**
     * 角色 -> 权限集合
     */
    private final Map<String, Set<String>> rolePermissions = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public Set<String> findRolesByUserId(@NonNull String userId) {
        return userRoles.getOrDefault(userId, Set.of());
    }

    @Override
    @NonNull
    public Set<String> findPermissionsByRole(@NonNull String role) {
        return rolePermissions.getOrDefault(role, Set.of());
    }

    @Override
    @NonNull
    public Set<String> findPermissionsByUserId(@NonNull String userId) {
        Set<String> permissions = new HashSet<>();
        Set<String> roles = findRolesByUserId(userId);
        for (String role : roles) {
            permissions.addAll(findPermissionsByRole(role));
        }
        return permissions;
    }

    @Override
    public void grantRole(@NonNull String userId, @NonNull String role) {
        userRoles.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(role);
        log.debug("Granted role: userId={}, role={}", userId, role);
    }

    @Override
    public void revokeRole(@NonNull String userId, @NonNull String role) {
        Set<String> roles = userRoles.get(userId);
        if (roles != null) {
            roles.remove(role);
        }
        log.debug("Revoked role: userId={}, role={}", userId, role);
    }

    @Override
    public void grantPermission(@NonNull String role, @NonNull String permission) {
        rolePermissions.computeIfAbsent(role, k -> ConcurrentHashMap.newKeySet()).add(permission);
        log.debug("Granted permission: role={}, permission={}", role, permission);
    }

    @Override
    public void revokePermission(@NonNull String role, @NonNull String permission) {
        Set<String> permissions = rolePermissions.get(role);
        if (permissions != null) {
            permissions.remove(permission);
        }
        log.debug("Revoked permission: role={}, permission={}", role, permission);
    }

    @Override
    public boolean hasRole(@NonNull String userId, @NonNull String role) {
        return findRolesByUserId(userId).contains(role);
    }

    @Override
    public boolean hasPermission(@NonNull String role, @NonNull String permission) {
        return findPermissionsByRole(role).contains(permission);
    }

    /**
     * 清空所有数据（用于测试）。
     */
    public void clear() {
        userRoles.clear();
        rolePermissions.clear();
    }

    /**
     * 创建预配置的测试实例。
     *
     * @return 测试实例
     */
    public static InMemoryRolePermissionStorage createTestInstance() {
        InMemoryRolePermissionStorage storage = new InMemoryRolePermissionStorage();

        // 配置 ADMIN 角色
        storage.grantPermission("ADMIN", "user:read");
        storage.grantPermission("ADMIN", "user:write");
        storage.grantPermission("ADMIN", "user:delete");
        storage.grantPermission("ADMIN", "role:read");
        storage.grantPermission("ADMIN", "role:write");

        // 配置 USER 角色
        storage.grantPermission("USER", "user:read");

        // 给测试用户分配角色
        storage.grantRole("admin-user", "ADMIN");
        storage.grantRole("normal-user", "USER");

        return storage;
    }
}