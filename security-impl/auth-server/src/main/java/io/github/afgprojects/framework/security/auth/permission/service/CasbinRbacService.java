package io.github.afgprojects.framework.security.auth.permission.service;

import io.github.afgprojects.framework.security.core.permission.RbacService;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 Casbin 的 RBAC 服务实现
 */
@Slf4j
public class CasbinRbacService implements RbacService {

    private final Enforcer enforcer;
    private final JdbcRoleService roleService;

    public CasbinRbacService(Enforcer enforcer, JdbcRoleService roleService) {
        this.enforcer = enforcer;
        this.roleService = roleService;
    }

    @Override
    public boolean hasPermission(@NonNull String userId, @NonNull String permission, @Nullable String tenantId) {
        String domain = tenantId != null ? tenantId : "default";
        String[] parts = permission.split(":");
        if (parts.length < 2) {
            return enforcer.enforce(userId, domain, permission, "access");
        }
        return enforcer.enforce(userId, domain, parts[0], parts[1]);
    }

    @Override
    public Set<String> getPermissions(@NonNull String userId, @Nullable String tenantId) {
        List<List<String>> permissions;
        if (tenantId != null) {
            permissions = enforcer.getImplicitPermissionsForUserInDomain(userId, tenantId);
        } else {
            permissions = enforcer.getImplicitPermissionsForUser(userId);
        }
        return permissions.stream()
            .map(p -> {
                if (p.size() >= 3) {
                    return p.get(1) + ":" + p.get(2);
                }
                return p.size() >= 2 ? p.get(1) : "";
            })
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getRoles(@NonNull String userId, @Nullable String tenantId) {
        List<String> roles;
        if (tenantId != null) {
            roles = enforcer.getImplicitRolesForUser(userId, tenantId);
        } else {
            roles = enforcer.getImplicitRolesForUser(userId);
        }
        return new HashSet<>(roles);
    }

    @Override
    public boolean hasRole(@NonNull String userId, @NonNull String roleCode, @Nullable String tenantId) {
        if (tenantId != null) {
            return enforcer.getRolesForUserInDomain(userId, tenantId).contains(roleCode);
        }
        return enforcer.hasRoleForUser(userId, roleCode);
    }

    @Override
    public void grantRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId) {
        String domain = tenantId != null ? tenantId : "default";
        enforcer.addRoleForUserInDomain(userId, role, domain);
        log.info("Granted role to user: userId={}, role={}, domain={}", userId, role, domain);
    }

    @Override
    public void revokeRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId) {
        String domain = tenantId != null ? tenantId : "default";
        enforcer.deleteRoleForUserInDomain(userId, role, domain);
        log.info("Revoked role from user: userId={}, role={}, domain={}", userId, role, domain);
    }

    @Override
    public void grantPermission(@NonNull String role, @NonNull String permission, @Nullable String tenantId) {
        String[] parts = permission.split(":");
        if (tenantId != null) {
            if (parts.length >= 2) {
                enforcer.addPermissionForUser(role, tenantId, parts[0], parts[1]);
            } else {
                enforcer.addPermissionForUser(role, tenantId, permission, "access");
            }
        } else {
            if (parts.length >= 2) {
                enforcer.addPermissionForUser(role, parts[0], parts[1]);
            } else {
                enforcer.addPermissionForUser(role, permission, "access");
            }
        }
        log.info("Granted permission to role: role={}, permission={}, tenantId={}", role, permission, tenantId);
    }

    @Override
    public void revokePermission(@NonNull String role, @NonNull String permission, @Nullable String tenantId) {
        String[] parts = permission.split(":");
        if (tenantId != null) {
            if (parts.length >= 2) {
                enforcer.deletePermissionForUser(role, tenantId, parts[0], parts[1]);
            } else {
                enforcer.deletePermissionForUser(role, tenantId, permission, "access");
            }
        } else {
            if (parts.length >= 2) {
                enforcer.deletePermissionForUser(role, parts[0], parts[1]);
            } else {
                enforcer.deletePermissionForUser(role, permission, "access");
            }
        }
        log.info("Revoked permission from role: role={}, permission={}, tenantId={}", role, permission, tenantId);
    }
}
