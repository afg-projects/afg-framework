package io.github.afgprojects.framework.security.permission.service;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.permission.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcRoleService {

    private final DataManager dataManager;
    private final Enforcer enforcer;

    public SecRole create(@NonNull SecRole role) {
        SecRole saved = dataManager.save(SecRole.class, role);
        log.info("Created role: {}", role.getRoleCode());
        return saved;
    }

    public SecRole update(@NonNull SecRole role) {
        return dataManager.save(SecRole.class, role);
    }

    public Optional<SecRole> findById(@NonNull Long id) {
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
    public void setRolePermissions(@NonNull Long roleId, @NonNull Set<Long> permissionIds, @Nullable String tenantId) {
        dataManager.findList(SecRolePermission.class,
            Conditions.builder(SecRolePermission.class)
                .eq(SecRolePermission::getRoleId, roleId)
                .build())
            .forEach(rp -> dataManager.deleteById(SecRolePermission.class, rp.getId()));

        for (Long permissionId : permissionIds) {
            SecRolePermission rp = new SecRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rp.setTenantId(tenantId);
            dataManager.save(SecRolePermission.class, rp);
        }
        log.info("Set role permissions: roleId={}, permissions={}", roleId, permissionIds);
    }

    public Set<Long> getRolePermissions(@NonNull Long roleId) {
        return dataManager.findList(SecRolePermission.class,
            Conditions.builder(SecRolePermission.class)
                .eq(SecRolePermission::getRoleId, roleId)
                .build())
            .stream()
            .map(SecRolePermission::getPermissionId)
            .collect(Collectors.toSet());
    }

    @Transactional
    public void setParentRole(@NonNull Long roleId, @NonNull Long parentRoleId, @Nullable String tenantId) {
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
    public void assignRoleToUser(@NonNull String userId, @NonNull Long roleId, @Nullable String tenantId) {
        var condition = Conditions.builder(SecUserRole.class)
            .eq(SecUserRole::getUserId, userId)
            .eq(SecUserRole::getRoleId, roleId);
        if (tenantId != null) {
            condition.eq(SecUserRole::getTenantId, tenantId);
        }

        if (dataManager.findOne(SecUserRole.class, condition.build()).isEmpty()) {
            SecUserRole userRole = new SecUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setTenantId(tenantId);
            dataManager.save(SecUserRole.class, userRole);
            log.info("Assigned role to user: userId={}, roleId={}", userId, roleId);
        }
    }

    @Transactional
    public void removeRoleFromUser(@NonNull String userId, @NonNull Long roleId, @Nullable String tenantId) {
        var condition = Conditions.builder(SecUserRole.class)
            .eq(SecUserRole::getUserId, userId)
            .eq(SecUserRole::getRoleId, roleId);
        if (tenantId != null) {
            condition.eq(SecUserRole::getTenantId, tenantId);
        }

        dataManager.findOne(SecUserRole.class, condition.build())
            .ifPresent(ur -> {
                dataManager.deleteById(SecUserRole.class, ur.getId());
                log.info("Removed role from user: userId={}, roleId={}", userId, roleId);
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

        Set<Long> roleIds = userRoles.stream()
            .map(SecUserRole::getRoleId)
            .collect(Collectors.toSet());

        return dataManager.findList(SecRole.class,
            Conditions.builder(SecRole.class)
                .in(SecRole::getId, roleIds)
                .build());
    }

    @Transactional
    public void delete(@NonNull Long id) {
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
}
