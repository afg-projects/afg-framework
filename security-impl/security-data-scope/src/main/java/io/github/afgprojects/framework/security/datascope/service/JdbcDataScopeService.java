package io.github.afgprojects.framework.security.datascope.service;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.security.datascope.entity.SecDataScope;
import io.github.afgprojects.framework.security.datascope.entity.SecUserDataScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据范围管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcDataScopeService {

    private final DataManager dataManager;
    private final JdbcDeptService deptService;

    public SecDataScope create(@NonNull SecDataScope dataScope) {
        return dataManager.save(SecDataScope.class, dataScope);
    }

    public Optional<SecDataScope> findById(@NonNull Long id) {
        return dataManager.findById(SecDataScope.class, id);
    }

    public List<SecDataScope> findAll(@Nullable String tenantId) {
        if (tenantId != null) {
            return dataManager.findList(SecDataScope.class,
                Conditions.builder(SecDataScope.class)
                    .eq(SecDataScope::getTenantId, tenantId)
                    .build());
        }
        return dataManager.findAll(SecDataScope.class);
    }

    public List<SecDataScope> getUserDataScopes(@NonNull String userId, @Nullable String tenantId) {
        var condition = Conditions.builder(SecUserDataScope.class)
            .eq(SecUserDataScope::getUserId, userId);
        if (tenantId != null) {
            condition.eq(SecUserDataScope::getTenantId, tenantId);
        }

        List<SecUserDataScope> userDataScopes = dataManager.findList(SecUserDataScope.class, condition.build());
        if (userDataScopes.isEmpty()) {
            return List.of();
        }

        Set<Long> scopeIds = userDataScopes.stream()
            .map(SecUserDataScope::getDataScopeId)
            .collect(Collectors.toSet());

        return dataManager.findList(SecDataScope.class,
            Conditions.builder(SecDataScope.class)
                .in(SecDataScope::getId, scopeIds)
                .build());
    }

    @Transactional
    public void setUserDataScopes(@NonNull String userId, @NonNull Set<Long> scopeIds, @Nullable String tenantId) {
        var condition = Conditions.builder(SecUserDataScope.class)
            .eq(SecUserDataScope::getUserId, userId);
        if (tenantId != null) {
            condition.eq(SecUserDataScope::getTenantId, tenantId);
        }

        dataManager.findList(SecUserDataScope.class, condition.build())
            .forEach(uds -> dataManager.deleteById(SecUserDataScope.class, uds.getId()));

        for (Long scopeId : scopeIds) {
            SecUserDataScope userDataScope = new SecUserDataScope();
            userDataScope.setUserId(userId);
            userDataScope.setDataScopeId(scopeId);
            userDataScope.setTenantId(tenantId);
            dataManager.save(SecUserDataScope.class, userDataScope);
        }
        log.info("Set user data scopes: userId={}, scopes={}", userId, scopeIds);
    }

    public List<DataScope> resolveUserDataScopes(@NonNull String userId, @Nullable String tenantId, @Nullable String resourceCode) {
        List<SecDataScope> dataScopes = getUserDataScopes(userId, tenantId);

        return dataScopes.stream()
            .filter(ds -> resourceCode == null || resourceCode.equals(ds.getResourceCode()))
            .map(ds -> {
                DataScope.Builder builder = DataScope.builder()
                    .scopeType(ds.getScopeTypeEnum());

                if (ds.getScopeTypeEnum() == DataScopeType.DEPT || ds.getScopeTypeEnum() == DataScopeType.DEPT_AND_CHILD) {
                    deptService.getPrimaryDept(userId, tenantId).ifPresent(dept -> {
                        builder.column("dept_id");
                    });
                } else if (ds.getScopeTypeEnum() == DataScopeType.SELF) {
                    builder.column("created_by");
                }

                return builder.build();
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(@NonNull Long id) {
        dataManager.findList(SecUserDataScope.class,
            Conditions.builder(SecUserDataScope.class)
                .eq(SecUserDataScope::getDataScopeId, id)
                .build())
            .forEach(uds -> dataManager.deleteById(SecUserDataScope.class, uds.getId()));

        dataManager.deleteById(SecDataScope.class, id);
        log.info("Deleted data scope: {}", id);
    }
}
