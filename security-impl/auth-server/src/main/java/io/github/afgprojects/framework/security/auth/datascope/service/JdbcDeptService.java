package io.github.afgprojects.framework.security.auth.datascope.service;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.datascope.entity.SecDept;
import io.github.afgprojects.framework.security.auth.datascope.entity.SecUserDept;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门管理服务。
 */
@Slf4j
public class JdbcDeptService {

    private final DataManager dataManager;

    public JdbcDeptService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public SecDept create(@NonNull SecDept dept) {
        return dataManager.save(SecDept.class, dept);
    }

    public SecDept update(@NonNull SecDept dept) {
        return dataManager.save(SecDept.class, dept);
    }

    public Optional<SecDept> findById(@NonNull String id) {
        return dataManager.findById(SecDept.class, id);
    }

    public List<SecDept> findAll(@NonNull String tenantId) {
        return dataManager.findList(SecDept.class,
            Conditions.builder(SecDept.class)
                .eq(SecDept::getTenantId, tenantId)
                .build());
    }

    public List<SecDept> getDeptTree(@NonNull String tenantId) {
        List<SecDept> allDepts = findAll(tenantId);
        return buildTree(allDepts, null);
    }

    private List<SecDept> buildTree(List<SecDept> allDepts, String parentId) {
        return allDepts.stream()
            .filter(d -> Objects.equals(d.getParentId(), parentId))
            .collect(Collectors.toList());
    }

    public List<SecDept> getUserDepts(@NonNull String userId, @Nullable String tenantId) {
        var condition = Conditions.builder(SecUserDept.class)
            .eq(SecUserDept::getUserId, userId);
        if (tenantId != null) {
            condition.eq(SecUserDept::getTenantId, tenantId);
        }

        List<SecUserDept> userDepts = dataManager.findList(SecUserDept.class, condition.build());
        if (userDepts.isEmpty()) {
            return List.of();
        }

        Set<String> deptIds = userDepts.stream()
            .map(SecUserDept::getDeptId)
            .collect(Collectors.toSet());

        return dataManager.findList(SecDept.class,
            Conditions.builder(SecDept.class)
                .in(SecDept::getId, deptIds)
                .build());
    }

    public Optional<SecDept> getPrimaryDept(@NonNull String userId, @Nullable String tenantId) {
        var condition = Conditions.builder(SecUserDept.class)
            .eq(SecUserDept::getUserId, userId)
            .eq(SecUserDept::getIsPrimary, true);
        if (tenantId != null) {
            condition.eq(SecUserDept::getTenantId, tenantId);
        }

        return dataManager.findOne(SecUserDept.class, condition.build())
            .flatMap(ud -> dataManager.findById(SecDept.class, ud.getDeptId()));
    }

    @Transactional
    public void setUserDept(@NonNull String userId, @NonNull String deptId, @Nullable String tenantId, boolean isPrimary) {
        var condition = Conditions.builder(SecUserDept.class)
            .eq(SecUserDept::getUserId, userId);
        if (tenantId != null) {
            condition.eq(SecUserDept::getTenantId, tenantId);
        }

        dataManager.findList(SecUserDept.class, condition.build())
            .forEach(ud -> dataManager.deleteById(SecUserDept.class, ud.getId()));

        SecUserDept userDept = new SecUserDept();
        userDept.setUserId(userId);
        userDept.setDeptId(deptId);
        userDept.setTenantId(tenantId);
        userDept.setIsPrimary(isPrimary);
        dataManager.save(SecUserDept.class, userDept);
        log.info("Set user dept: userId={}, deptId={}", userId, deptId);
    }

    public Set<String> getChildDeptIds(@NonNull String deptId, @NonNull String tenantId) {
        Set<String> result = new HashSet<>();
        result.add(deptId);
        collectChildDeptIds(deptId, tenantId, result);
        return result;
    }

    private void collectChildDeptIds(String parentId, String tenantId, Set<String> result) {
        List<SecDept> children = dataManager.findList(SecDept.class,
            Conditions.builder(SecDept.class)
                .eq(SecDept::getParentId, parentId)
                .eq(SecDept::getTenantId, tenantId)
                .build());

        for (SecDept child : children) {
            result.add(child.getId());
            collectChildDeptIds(child.getId(), tenantId, result);
        }
    }

    @Transactional
    public void delete(@NonNull String id) {
        dataManager.findList(SecUserDept.class,
            Conditions.builder(SecUserDept.class)
                .eq(SecUserDept::getDeptId, id)
                .build())
            .forEach(ud -> dataManager.deleteById(SecUserDept.class, ud.getId()));

        dataManager.deleteById(SecDept.class, id);
        log.info("Deleted dept: {}", id);
    }
}
