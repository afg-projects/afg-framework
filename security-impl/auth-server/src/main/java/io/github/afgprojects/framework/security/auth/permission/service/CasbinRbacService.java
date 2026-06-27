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
        // 权限码格式：module:resource:action（三段式）
        // 拆分为 Casbin 的 obj(module:resource) 和 act(action)
        int lastColon = permission.lastIndexOf(':');
        if (lastColon > 0) {
            String obj = permission.substring(0, lastColon);
            String act = permission.substring(lastColon + 1);
            return enforcer.enforce(userId, domain, obj, act);
        }
        return enforcer.enforce(userId, domain, permission, "access");
    }

    @Override
    public Set<String> getPermissions(@NonNull String userId, @Nullable String tenantId) {
        List<List<String>> permissions;
        if (tenantId != null) {
            permissions = enforcer.getImplicitPermissionsForUserInDomain(userId, tenantId);
        } else {
            permissions = enforcer.getImplicitPermissionsForUser(userId);
        }
        // Casbin 策略格式：p = sub, dom, obj, act
        // obj = module:resource, act = action
        // 拼接为三段式：obj:act = module:resource:action
        return permissions.stream()
            .map(p -> {
                if (p.size() >= 4) {
                    return p.get(2) + ":" + p.get(3);
                }
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
        // 权限码格式：module:resource:action（三段式）
        // 拆分为 Casbin 的 obj(module:resource) 和 act(action)
        int lastColon = permission.lastIndexOf(':');
        String obj = lastColon > 0 ? permission.substring(0, lastColon) : permission;
        String act = lastColon > 0 ? permission.substring(lastColon + 1) : "access";

        if (tenantId != null) {
            enforcer.addPermissionForUser(role, tenantId, obj, act);
        } else {
            enforcer.addPermissionForUser(role, obj, act);
        }
        log.info("Granted permission to role: role={}, permission={}, tenantId={}", role, permission, tenantId);
    }

    @Override
    public void revokePermission(@NonNull String role, @NonNull String permission, @Nullable String tenantId) {
        int lastColon = permission.lastIndexOf(':');
        String obj = lastColon > 0 ? permission.substring(0, lastColon) : permission;
        String act = lastColon > 0 ? permission.substring(lastColon + 1) : "access";

        if (tenantId != null) {
            enforcer.deletePermissionForUser(role, tenantId, obj, act);
        } else {
            enforcer.deletePermissionForUser(role, obj, act);
        }
        log.info("Revoked permission from role: role={}, permission={}, tenantId={}", role, permission, tenantId);
    }
}
