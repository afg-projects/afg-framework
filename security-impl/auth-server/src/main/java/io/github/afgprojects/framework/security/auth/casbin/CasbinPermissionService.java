package io.github.afgprojects.framework.security.auth.casbin;

import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.permission.PermissionService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Casbin 权限服务实现。
 *
 * <p>基于 Casbin 实现 ABAC（基于属性的访问控制）和 RBAC 混合模式。
 *
 * <h3>模型示例</h3>
 * <pre>
 * [request_definition]
 * r = sub, obj, act
 *
 * [policy_definition]
 * p = sub, obj, act
 *
 * [role_definition]
 * g = _, _
 *
 * [policy_effect]
 * e = some(where (p.eft == allow))
 *
 * [matchers]
 * m = g(r.sub, p.sub) &amp;&amp; r.obj == p.obj &amp;&amp; r.act == p.act
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * Enforcer enforcer = new Enforcer("model.conf", "policy.csv");
 * CasbinPermissionService permissionService = new CasbinPermissionService(enforcer);
 *
 * // 检查权限
 * boolean allowed = permissionService.check("user-001", "user:read", "user-002");
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class CasbinPermissionService implements PermissionService {

    private final Enforcer enforcer;

    /**
     * 构造函数。
     *
     * @param enforcer Casbin 执行器
     */
    public CasbinPermissionService(@NonNull Enforcer enforcer) {
        this.enforcer = enforcer;
    }

    @Override
    public boolean hasRole(@NonNull String userId, @NonNull String role) {
        return enforcer.hasRoleForUser(userId, role);
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
        // 将权限拆分为资源和操作
        String[] parts = permission.split(":");
        if (parts.length != 2) {
            log.warn("Invalid permission format: {}", permission);
            return false;
        }
        String resource = parts[0];
        String action = parts[1];

        // Casbin 检查：sub=userId, obj=resource, act=action
        return enforcer.enforce(userId, resource, action);
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
        // ABAC 模式：检查用户对特定资源的操作权限
        String resource = resourceId != null ? resourceId : "*";
        return enforcer.enforce(userId, resource, action);
    }

    @Override
    public boolean check(@NonNull String userId, @NonNull String action, @Nullable String resourceId, @Nullable String tenantId) {
        // 带租户上下文的权限检查
        // 可以在 Casbin 模型中扩展租户维度
        String resource = resourceId != null ? resourceId : "*";
        if (tenantId != null) {
            // 使用租户作为资源前缀
            resource = tenantId + ":" + resource;
        }
        return enforcer.enforce(userId, resource, action);
    }

    @Override
    @NonNull
    public Set<String> getRoles(@NonNull String userId) {
        List<String> roles = enforcer.getRolesForUser(userId);
        return roles.stream().collect(Collectors.toSet());
    }

    @Override
    @NonNull
    public Set<String> getPermissions(@NonNull String userId) {
        // 获取用户的所有权限策略
        List<List<String>> policies = enforcer.getImplicitPermissionsForUser(userId);
        return policies.stream()
                .map(policy -> {
                    if (policy.size() >= 3) {
                        return policy.get(1) + ":" + policy.get(2);
                    }
                    return null;
                })
                .filter(p -> p != null)
                .collect(Collectors.toSet());
    }

    @Override
    public void grantRole(@NonNull String userId, @NonNull String role) {
        enforcer.addRoleForUser(userId, role);
        log.info("Granted role via Casbin: userId={}, role={}", userId, role);
    }

    @Override
    public void revokeRole(@NonNull String userId, @NonNull String role) {
        enforcer.deleteRoleForUser(userId, role);
        log.info("Revoked role via Casbin: userId={}, role={}", userId, role);
    }

    @Override
    public void grantPermission(@NonNull String role, @NonNull String permission) {
        String[] parts = permission.split(":");
        if (parts.length != 2) {
            log.warn("Invalid permission format: {}", permission);
            return;
        }
        String resource = parts[0];
        String action = parts[1];

        // 添加策略：p = role, resource, action
        enforcer.addPolicy(role, resource, action);
        log.info("Granted permission via Casbin: role={}, permission={}", role, permission);
    }

    @Override
    public void revokePermission(@NonNull String role, @NonNull String permission) {
        String[] parts = permission.split(":");
        if (parts.length != 2) {
            log.warn("Invalid permission format: {}", permission);
            return;
        }
        String resource = parts[0];
        String action = parts[1];

        enforcer.removePolicy(role, resource, action);
        log.info("Revoked permission via Casbin: role={}, permission={}", role, permission);
    }

    /**
     * 添加自定义策略。
     *
     * @param sub 主体（用户/角色）
     * @param obj 对象（资源）
     * @param act 操作
     */
    public void addPolicy(@NonNull String sub, @NonNull String obj, @NonNull String act) {
        enforcer.addPolicy(sub, obj, act);
        log.info("Added policy: sub={}, obj={}, act={}", sub, obj, act);
    }

    /**
     * 删除自定义策略。
     *
     * @param sub 主体（用户/角色）
     * @param obj 对象（资源）
     * @param act 操作
     */
    public void removePolicy(@NonNull String sub, @NonNull String obj, @NonNull String act) {
        enforcer.removePolicy(sub, obj, act);
        log.info("Removed policy: sub={}, obj={}, act={}", sub, obj, act);
    }

    /**
     * 获取 Casbin 执行器（用于高级操作）。
     *
     * @return Casbin 执行器
     */
    public Enforcer getEnforcer() {
        return enforcer;
    }
}