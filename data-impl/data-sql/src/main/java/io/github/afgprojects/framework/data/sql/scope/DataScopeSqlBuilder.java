package io.github.afgprojects.framework.data.sql.scope;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限 SQL 构建器
 * <p>
 * 将 {@link DataScope} 列表转换为 SQL WHERE 过滤条件。
 * 根据 {@link DataScopeType} 生成不同粒度的数据过滤 SQL。
 *
 * @author afg
 */
@Slf4j
public class DataScopeSqlBuilder {

    private final @NonNull DataScopeContextProvider contextProvider;

    public DataScopeSqlBuilder(@NonNull DataScopeContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    /**
     * 构建 DataScope 过滤 SQL
     *
     * @param dataScopes 数据权限列表
     * @param dialect    数据库方言
     * @return SQL 构建结果，如果不需要过滤则返回 null
     */
    public @Nullable SqlResult buildSql(@NonNull List<DataScope> dataScopes, @NonNull Dialect dialect) {
        if (dataScopes.isEmpty()) {
            return null;
        }

        DataScopeUserContext userContext = provideUserContext();

        // 管理员跳过过滤
        if (userContext.isAllDataPermission()) {
            log.debug("Admin user detected, skipping data scope filter");
            return null;
        }

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        for (DataScope scope : dataScopes) {
            SqlResult result = buildSingleScope(scope, userContext, dialect);
            if (result != null) {
                conditions.add(result.sql());
                params.addAll(result.parameters());
            }
        }

        if (conditions.isEmpty()) {
            return null;
        }

        String sql = conditions.stream()
                .map(c -> "(" + c + ")")
                .collect(Collectors.joining(" AND "));

        return new SqlResult(sql, params);
    }

    /**
     * 构建单个 DataScope 的 SQL
     */
    private @Nullable SqlResult buildSingleScope(@NonNull DataScope scope,
                                                  @NonNull DataScopeUserContext userContext,
                                                  @NonNull Dialect dialect) {
        DataScopeType scopeType = scope.scopeType();
        String column = scope.column();

        // 考虑别名前缀
        String quotedColumn;
        if (scope.aliasPrefix() != null && !scope.aliasPrefix().isEmpty()) {
            quotedColumn = dialect.quoteIdentifier(scope.aliasPrefix()) + "." + dialect.quoteIdentifier(column);
        } else {
            quotedColumn = dialect.quoteIdentifier(column);
        }

        return switch (scopeType) {
            case ALL -> null; // 不过滤

            case SELF -> {
                Long userId = userContext.getUserId();
                if (userId == null) {
                    log.warn("SELF data scope requires userId, but current user context has no userId");
                    yield new SqlResult(quotedColumn + " = " + Long.MIN_VALUE, List.of());
                }
                yield new SqlResult(quotedColumn + " = ?", List.of(userId));
            }

            case DEPT -> {
                Long deptId = userContext.getDeptId();
                if (deptId == null) {
                    log.warn("DEPT data scope requires deptId, but current user context has no deptId");
                    yield new SqlResult(quotedColumn + " = " + Long.MIN_VALUE, List.of());
                }
                yield new SqlResult(quotedColumn + " = ?", List.of(deptId));
            }

            case DEPT_AND_CHILD -> {
                List<Long> deptIds = new ArrayList<>();
                Long deptId = userContext.getDeptId();
                if (deptId != null) {
                    deptIds.add(deptId);
                }
                if (userContext.getAccessibleDeptIds() != null) {
                    deptIds.addAll(userContext.getAccessibleDeptIds());
                }
                if (deptIds.isEmpty()) {
                    log.warn("DEPT_AND_CHILD data scope requires accessibleDeptIds, but current user context has none");
                    yield new SqlResult(quotedColumn + " = " + Long.MIN_VALUE, List.of());
                }
                if (deptIds.size() == 1) {
                    yield new SqlResult(quotedColumn + " = ?", List.of(deptIds.get(0)));
                } else {
                    String placeholders = deptIds.stream().map(d -> "?").collect(Collectors.joining(", "));
                    yield new SqlResult(quotedColumn + " IN (" + placeholders + ")", new ArrayList<>(deptIds));
                }
            }

            case CUSTOM -> {
                String customCondition = scope.customCondition();
                if (customCondition == null || customCondition.isEmpty()) {
                    yield null;
                }
                // 使用 DataScopeProcessor 解析占位符
                DataScopeProcessor processor = new DataScopeProcessor(contextProvider);
                String resolved = processor.resolvePlaceholders(customCondition);
                yield new SqlResult(resolved, List.of());
            }
        };
    }

    /**
     * 获取用户上下文
     */
    private DataScopeUserContext provideUserContext() {
        DataScopeUserContext context = contextProvider.provide();
        return context != null ? context : DataScopeUserContext.empty();
    }

    /**
     * SQL 构建结果
     */
    public record SqlResult(@NonNull String sql, @NonNull List<Object> parameters) {}
}