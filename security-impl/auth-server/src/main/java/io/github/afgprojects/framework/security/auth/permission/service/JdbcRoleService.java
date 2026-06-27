package io.github.afgprojects.framework.security.auth.permission.service;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.casbin.enforcer.CasbinAfgEnforcer;
import io.github.afgprojects.framework.security.auth.permission.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理服务
 *
 * <p>管理操作双写到 sec_role_permission/sec_user_role 和 sec_casbin_rule，
 * 确保管理操作和 Casbin 运行时策略保持一致。
 */
@Slf4j
public class JdbcRoleService {

    private final DataManager dataManager;
    private final CasbinAfgEnforcer casbinEnforcer;

    public JdbcRoleService(DataManager dataManager, CasbinAfgEnforcer casbinEnforcer) {
        this.dataManager = dataManager;
        this.casbinEnforcer = casbinEnforcer;
    }

    public SecRole create(@NonNull SecRole role) {
        SecRole saved = dataManager.save(SecRole.class, role);
        log.info("Created role: {}", role.getRoleCode());
        return saved;
    }

    public SecRole update(@NonNull SecRole role) {
        return dataManager.save(SecRole.class, role);
    }

    public Optional<SecRole> findById(@NonNull String id) {
        return dataManager.findById(SecRole.class, id);
    }

    public Optional<SecRole> findByCode(@NonNull String roleCode, @Nullable String tenantId) {
        var condition = Conditions.builder(SecRole.class)
            .eq(SecRole::getRoleCode, roleCode);
        if (tenantId != null) {
            condition.eq(SecRole::getTenantId, tenantId);
        }
        return dataManager.findOne(SecRole.class, condition.build());
    }

    public List<SecRole> findAll(@Nullable String tenantId) {
        if (tenantId != null) {
            return dataManager.findList(SecRole.class,
                Conditions.builder(SecRole.class)
                    .eq(SecRole::getTenantId, tenantId)
                    .build());
        }
        return dataManager.findAll(SecRole.class);
    }

    @Transactional
    public void setRolePermissions(@NonNull String roleId, @NonNull Set<String> permissionIds, @Nullable String tenantId) {
        String domain = tenantId != null ? tenantId : "default";

        // 1. 查找角色信息
        SecRole role = dataManager.findById(SecRole.class, roleId).orElse(null);
        if (role == null) {
            log.warn("Role not found: roleId={}", roleId);
            return;
        }
        String roleCode = role.getRoleCode();

        // 2. 删除旧的 sec_role_permission 记录
        dataManager.findList(SecRolePermission.class,
            Conditions.builder(SecRolePermission.class)
                .eq(SecRolePermission::getRoleId, roleId)
                .build())
            .forEach(rp -> dataManager.deleteById(SecRolePermission.class, rp.getId()));

        // 3. 删除该角色在 sec_casbin_rule 中的旧 p 策略
        dataManager.findList(SecCasbinRule.class,
            Conditions.builder(SecCasbinRule.class)
                .eq(SecCasbinRule::getPtype, "p")
                .eq(SecCasbinRule::getV0, roleCode)
                .eq(SecCasbinRule::getV1, domain)
                .build())
            .forEach(rule -> dataManager.deleteById(SecCasbinRule.class, rule.getId()));

        // 4. 查找权限码并写入新的 sec_role_permission + sec_casbin_rule
        for (String permissionId : permissionIds) {
            // 写入 sec_role_permission
            SecRolePermission rp = new SecRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rp.setTenantId(tenantId);
            dataManager.save(SecRolePermission.class, rp);

            // 查找 permission_code 用于 Casbin 策略
            dataManager.findById(SecPermission.class, permissionId).ifPresent(perm -> {
                String permissionCode = perm.getPermissionCode();
                String[] parts = splitPermissionCode(permissionCode);
                String obj = parts[0];
                String act = parts[1];

                // 写入 sec_casbin_rule
                SecCasbinRule casbinRule = SecCasbinRule.createPolicy(roleCode, domain, obj, act);
                dataManager.save(SecCasbinRule.class, casbinRule);
            });
        }

        // 5. 重新加载 Casbin 策略
        casbinEnforcer.reloadPolicies();
        log.info("Set role permissions (dual-write): roleId={}, roleCode={}, permissions={}", roleId, roleCode, permissionIds);
    }

    public Set<String> getRolePermissions(@NonNull String roleId) {
        return dataManager.findList(SecRolePermission.class,
            Conditions.builder(SecRolePermission.class)
                .eq(SecRolePermission::getRoleId, roleId)
                .build())
            .stream()
            .map(SecRolePermission::getPermissionId)
            .collect(Collectors.toSet());
    }

    @Transactional
    public void setParentRole(@NonNull String roleId, @NonNull String parentRoleId, @Nullable String tenantId) {
        dataManager.findList(SecRoleHierarchy.class,
            Conditions.builder(SecRoleHierarchy.class)
                .eq(SecRoleHierarchy::getRoleId, roleId)
                .build())
            .forEach(rh -> dataManager.deleteById(SecRoleHierarchy.class, rh.getId()));

        SecRoleHierarchy hierarchy = new SecRoleHierarchy();
        hierarchy.setRoleId(roleId);
        hierarchy.setParentRoleId(parentRoleId);
        hierarchy.setTenantId(tenantId);
        dataManager.save(SecRoleHierarchy.class, hierarchy);
        log.info("Set role hierarchy: roleId={}, parentId={}", roleId, parentRoleId);
    }

    @Transactional
    public void assignRoleToUser(@NonNull String userId, @NonNull String roleId, @Nullable String tenantId) {
        String domain = tenantId != null ? tenantId : "default";

        var condition = Conditions.builder(SecUserRole.class)
            .eq(SecUserRole::getUserId, userId)
            .eq(SecUserRole::getRoleId, roleId);
        if (tenantId != null) {
            condition.eq(SecUserRole::getTenantId, tenantId);
        }

        if (dataManager.findOne(SecUserRole.class, condition.build()).isEmpty()) {
            // 写入 sec_user_role
            SecUserRole userRole = new SecUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setTenantId(tenantId);
            dataManager.save(SecUserRole.class, userRole);

            // 查找角色 code 用于 Casbin grouping 策略
            dataManager.findById(SecRole.class, roleId).ifPresent(role -> {
                String roleCode = role.getRoleCode();
                // 写入 sec_casbin_rule g 策略
                SecCasbinRule casbinRule = SecCasbinRule.createRole(userId, domain, roleCode);
                dataManager.save(SecCasbinRule.class, casbinRule);
            });

            // 重新加载 Casbin 策略
            casbinEnforcer.reloadPolicies();
            log.info("Assigned role to user (dual-write): userId={}, roleId={}", userId, roleId);
        }
    }

    @Transactional
    public void removeRoleFromUser(@NonNull String userId, @NonNull String roleId, @Nullable String tenantId) {
        String domain = tenantId != null ? tenantId : "default";

        var condition = Conditions.builder(SecUserRole.class)
            .eq(SecUserRole::getUserId, userId)
            .eq(SecUserRole::getRoleId, roleId);
        if (tenantId != null) {
            condition.eq(SecUserRole::getTenantId, tenantId);
        }

        dataManager.findOne(SecUserRole.class, condition.build())
            .ifPresent(ur -> {
                // 删除 sec_user_role
                dataManager.deleteById(SecUserRole.class, ur.getId());

                // 查找角色 code 并删除 sec_casbin_rule g 策略
                dataManager.findById(SecRole.class, roleId).ifPresent(role -> {
                    String roleCode = role.getRoleCode();
                    dataManager.findList(SecCasbinRule.class,
                        Conditions.builder(SecCasbinRule.class)
                            .eq(SecCasbinRule::getPtype, "g")
                            .eq(SecCasbinRule::getV0, userId)
                            .eq(SecCasbinRule::getV1, domain)
                            .eq(SecCasbinRule::getV2, roleCode)
                            .build())
                        .forEach(rule -> dataManager.deleteById(SecCasbinRule.class, rule.getId()));
                });

                // 重新加载 Casbin 策略
                casbinEnforcer.reloadPolicies();
                log.info("Removed role from user (dual-write): userId={}, roleId={}", userId, roleId);
            });
    }

    public List<SecRole> getUserRoles(@NonNull String userId, @Nullable String tenantId) {
        var condition = Conditions.builder(SecUserRole.class)
            .eq(SecUserRole::getUserId, userId);
        if (tenantId != null) {
            condition.eq(SecUserRole::getTenantId, tenantId);
        }

        List<SecUserRole> userRoles = dataManager.findList(SecUserRole.class, condition.build());
        if (userRoles.isEmpty()) {
            return List.of();
        }

        Set<String> roleIds = userRoles.stream()
            .map(SecUserRole::getRoleId)
            .collect(Collectors.toSet());

        return dataManager.findList(SecRole.class,
            Conditions.builder(SecRole.class)
                .in(SecRole::getId, roleIds)
                .build());
    }

    @Transactional
    public void delete(@NonNull String id) {
        dataManager.findList(SecRolePermission.class,
            Conditions.builder(SecRolePermission.class)
                .eq(SecRolePermission::getRoleId, id)
                .build())
            .forEach(rp -> dataManager.deleteById(SecRolePermission.class, rp.getId()));

        dataManager.findList(SecUserRole.class,
            Conditions.builder(SecUserRole.class)
                .eq(SecUserRole::getRoleId, id)
                .build())
            .forEach(ur -> dataManager.deleteById(SecUserRole.class, ur.getId()));

        dataManager.findList(SecRoleHierarchy.class,
            Conditions.builder(SecRoleHierarchy.class)
                .eq(SecRoleHierarchy::getRoleId, id)
                .build())
            .forEach(rh -> dataManager.deleteById(SecRoleHierarchy.class, rh.getId()));

        dataManager.deleteById(SecRole.class, id);
        log.info("Deleted role: {}", id);
    }

    /**
     * 拆分权限码为 Casbin 的 obj 和 act。
     *
     * <p>权限码格式：module:resource:action（三段式）
     * <p>拆分规则：obj = module:resource（最后一个冒号之前），act = action（最后一个冒号之后）
     *
     * @param permissionCode 权限码，如 "system:user:list"
     * @return [obj, act]，如 ["system:user", "list"]
     */
    private String[] splitPermissionCode(String permissionCode) {
        int lastColon = permissionCode.lastIndexOf(':');
        if (lastColon > 0) {
            return new String[]{
                permissionCode.substring(0, lastColon),
                permissionCode.substring(lastColon + 1)
            };
        }
        return new String[]{permissionCode, "access"};
    }
}
