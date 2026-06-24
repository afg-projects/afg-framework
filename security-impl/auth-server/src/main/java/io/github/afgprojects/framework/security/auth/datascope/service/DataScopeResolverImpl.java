package io.github.afgprojects.framework.security.auth.datascope.service;

import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.security.auth.datascope.entity.SecDept;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 数据范围解析器实现
 */
@Slf4j
public class DataScopeResolverImpl {

    private final JdbcDeptService deptService;
    private final JdbcDataScopeService dataScopeService;

    public DataScopeResolverImpl(JdbcDeptService deptService, JdbcDataScopeService dataScopeService) {
        this.deptService = deptService;
        this.dataScopeService = dataScopeService;
    }

    public List<DataScope> resolve(@NonNull String userId,
                                   @Nullable String tenantId,
                                   @Nullable String resourceCode,
                                   @Nullable String defaultField) {
        List<DataScope> dataScopes = dataScopeService.resolveUserDataScopes(userId, tenantId, resourceCode);

        if (dataScopes.isEmpty() && defaultField != null) {
            return List.of(DataScope.of("", defaultField, DataScopeType.ALL));
        }

        return dataScopes;
    }

    public Set<String> getAccessibleDeptIds(@NonNull String userId, @Nullable String tenantId) {
        List<DataScope> dataScopes = dataScopeService.resolveUserDataScopes(userId, tenantId, null);

        for (DataScope scope : dataScopes) {
            if (scope.scopeType() == DataScopeType.ALL) {
                return Set.of();
            }

            if (scope.scopeType() == DataScopeType.DEPT) {
                Optional<SecDept> dept = deptService.getPrimaryDept(userId, tenantId);
                return dept.map(d -> Set.of(d.getId())).orElse(Set.of());
            }

            if (scope.scopeType() == DataScopeType.DEPT_AND_CHILD) {
                Optional<SecDept> dept = deptService.getPrimaryDept(userId, tenantId);
                if (dept.isPresent() && tenantId != null) {
                    return deptService.getChildDeptIds(dept.get().getId(), tenantId);
                }
                return Set.of();
            }
        }

        return Set.of();
    }
}
