package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.query.AggregateFunction;
import io.github.afgprojects.framework.data.core.query.AggregateQuery;
import io.github.afgprojects.framework.data.core.query.AggregateReference;
import io.github.afgprojects.framework.data.core.query.AggregateResult;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import io.github.afgprojects.framework.data.sql.scope.DataScopeSqlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 聚合查询 JDBC 实现
 *
 * @author afg
 */
@Slf4j
public class JdbcAggregateQuery<T> implements AggregateQuery<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;
    private final JdbcEntityProxy<T> parentProxy;

    private final Set<String> groupByFields = new LinkedHashSet<>();
    private final List<AggregateReference> aggregates = new ArrayList<>();
    private Condition whereCondition;
    private Condition havingCondition;
    private Sort sort;
    private final List<DataScope> dataScopes = new ArrayList<>();
    private boolean includeDeleted = false;
    private String tenantId;

    public JdbcAggregateQuery(JdbcEntityProxy<T> parentProxy) {
        this.parentProxy = parentProxy;
        this.entityClass = parentProxy.getEntityClass();
        this.jdbcClient = parentProxy.getJdbcClient();
        this.dialect = parentProxy.getDialect();
        this.metadata = parentProxy.dataManager.getEntityMetadata(entityClass);
    }

    // ==================== GROUP BY ====================

    @Override
    public @NonNull AggregateQuery<T> groupBy(@NonNull String... fields) {
        for (String field : fields) {
            groupByFields.add(resolveColumnName(field));
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NonNull AggregateQuery<T> groupBy(@NonNull SFunction<T, ?>... getters) {
        for (SFunction<T, ?> getter : getters) {
            String fieldName = Conditions.getFieldName(getter);
            groupByFields.add(resolveColumnName(fieldName));
        }
        return this;
    }

    // ==================== 聚合函数（字符串字段名） ====================

    @Override
    public @NonNull AggregateQuery<T> count(@NonNull String field, @NonNull String alias) {
        aggregates.add(AggregateReference.of(AggregateFunction.COUNT, resolveColumnName(field), alias));
        return this;
    }

    @Override
    public @NonNull AggregateQuery<T> count(@NonNull String alias) {
        aggregates.add(AggregateReference.countAll(alias));
        return this;
    }

    @Override
    public @NonNull AggregateQuery<T> countDistinct(@NonNull String field, @NonNull String alias) {
        aggregates.add(AggregateReference.of(AggregateFunction.COUNT_DISTINCT, resolveColumnName(field), alias));
        return this;
    }

    @Override
    public @NonNull AggregateQuery<T> sum(@NonNull String field, @NonNull String alias) {
        aggregates.add(AggregateReference.of(AggregateFunction.SUM, resolveColumnName(field), alias));
        return this;
    }

    @Override
    public @NonNull AggregateQuery<T> avg(@NonNull String field, @NonNull String alias) {
        aggregates.add(AggregateReference.of(AggregateFunction.AVG, resolveColumnName(field), alias));
        return this;
    }

    @Override
    public @NonNull AggregateQuery<T> max(@NonNull String field, @NonNull String alias) {
        aggregates.add(AggregateReference.of(AggregateFunction.MAX, resolveColumnName(field), alias));
        return this;
    }

    @Override
    public @NonNull AggregateQuery<T> min(@NonNull String field, @NonNull String alias) {
        aggregates.add(AggregateReference.of(AggregateFunction.MIN, resolveColumnName(field), alias));
        return this;
    }

    // ==================== 聚合函数（Lambda） ====================

    @Override
    public <R> @NonNull AggregateQuery<T> count(@NonNull SFunction<T, R> getter, @NonNull String alias) {
        return count(Conditions.getFieldName(getter), alias);
    }

    @Override
    public <R extends Number> @NonNull AggregateQuery<T> sum(@NonNull SFunction<T, R> getter, @NonNull String alias) {
        return sum(Conditions.getFieldName(getter), alias);
    }

    @Override
    public <R extends Number> @NonNull AggregateQuery<T> avg(@NonNull SFunction<T, R> getter, @NonNull String alias) {
        return avg(Conditions.getFieldName(getter), alias);
    }

    @Override
    public <R extends Comparable<?>> @NonNull AggregateQuery<T> max(@NonNull SFunction<T, R> getter, @NonNull String alias) {
        return max(Conditions.getFieldName(getter), alias);
    }

    @Override
    public <R extends Comparable<?>> @NonNull AggregateQuery<T> min(@NonNull SFunction<T, R> getter, @NonNull String alias) {
        return min(Conditions.getFieldName(getter), alias);
    }

    // ==================== WHERE / HAVING ====================

    @Override
    public @NonNull AggregateQuery<T> where(@NonNull Condition condition) {
        this.whereCondition = condition;
        return this;
    }

    @Override
    public @NonNull AggregateQuery<T> having(@NonNull Condition condition) {
        this.havingCondition = condition;
        return this;
    }

    // ==================== 排序 ====================

    @Override
    public @NonNull AggregateQuery<T> orderBy(@NonNull Sort sort) {
        this.sort = sort;
        return this;
    }

    // ==================== 数据权限 ====================

    @Override
    public @NonNull AggregateQuery<T> withDataScope(@NonNull DataScope scope) {
        this.dataScopes.add(scope);
        return this;
    }

    // ==================== 软删除 ====================

    @Override
    public @NonNull AggregateQuery<T> includeDeleted() {
        this.includeDeleted = true;
        return this;
    }

    // ==================== 执行 ====================

    @Override
    public @NonNull List<AggregateResult> list() {
        if (aggregates.isEmpty()) {
            throw new IllegalStateException("At least one aggregate function must be specified");
        }

        String sql = buildAggregateSql(false);
        List<Object> params = collectParams();
        log.debug("Aggregate query SQL: {}", sql);

        return jdbcClient.sql(sql)
                .params(params)
                .query(new AggregateResultRowMapper())
                .list();
    }

    @Override
    public @NonNull AggregateResult single() {
        if (aggregates.isEmpty()) {
            throw new IllegalStateException("At least one aggregate function must be specified");
        }

        String sql = buildAggregateSql(true);
        List<Object> params = collectParams();
        log.debug("Aggregate single query SQL: {}", sql);

        AggregateResult result = jdbcClient.sql(sql)
                .params(params)
                .query(new AggregateResultRowMapper())
                .single();
        return result != null ? result : new AggregateResult(Map.of());
    }

    // ==================== SQL 构建 ====================

    private String buildAggregateSql(boolean isSingle) {
        StringBuilder sql = new StringBuilder("SELECT ");

        // SELECT 子句：GROUP BY 字段 + 聚合表达式
        List<String> selectItems = new ArrayList<>();
        if (!isSingle) {
            for (String field : groupByFields) {
                selectItems.add(dialect.quoteIdentifier(field));
            }
        }
        for (AggregateReference ref : aggregates) {
            selectItems.add(ref.toSqlExpression() + " AS " + dialect.quoteIdentifier(ref.alias()));
        }
        sql.append(String.join(", ", selectItems));

        // FROM 子句
        sql.append(" FROM ").append(dialect.quoteIdentifier(metadata.getTableName()));

        // WHERE 子句
        buildWhereClause(sql);

        // GROUP BY 子句（全局统计时跳过）
        if (!isSingle && !groupByFields.isEmpty()) {
            sql.append(" GROUP BY ");
            sql.append(groupByFields.stream()
                    .map(dialect::quoteIdentifier)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
        }

        // HAVING 子句
        if (!isSingle && havingCondition != null && !havingCondition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
            ConditionToSqlConverter.SqlResult result = converter.convert(havingCondition);
            String havingSql = result.sql();

            // 替换聚合别名为聚合表达式
            for (AggregateReference ref : aggregates) {
                String alias = ref.alias();
                String expression = ref.toSqlExpression();
                havingSql = havingSql.replaceAll("(?<![a-zA-Z0-9_])" + Pattern.quote(alias) + "(?![a-zA-Z0-9_])", expression);
            }

            sql.append(" HAVING ").append(havingSql);
        }

        // ORDER BY 子句
        if (sort != null && sort.isSorted()) {
            sql.append(" ORDER BY ").append(OrderByHelper.buildOrderByClause(sort, dialect, metadata));
        }

        return sql.toString();
    }

    /**
     * 构建 WHERE 子句（条件 + 软删除 + 租户 + 数据权限）
     */
    private void buildWhereClause(StringBuilder sql) {
        boolean hasWhere = false;
        if (whereCondition != null && !whereCondition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
            ConditionToSqlConverter.SqlResult result = converter.convert(whereCondition);
            sql.append(" WHERE ").append(result.sql());
            hasWhere = true;
        }

        // 软删除过滤
        if (!includeDeleted && (metadata.hasTrait(EntityTrait.SOFT_DELETABLE) || metadata.hasTrait(EntityTrait.TIMESTAMP_SOFT_DELETABLE))) {
            String filter = parentProxy.getSoftDeleteHandler().getSoftDeleteFilterCondition();
            if (filter != null) {
                sql.append(hasWhere ? " AND " : " WHERE ").append(filter);
                hasWhere = true;
            }
        }

        // 租户过滤
        String effectiveTenantId = resolveEffectiveTenantId();
        if (effectiveTenantId != null && metadata.getTenantField() != null) {
            String tenantColumn = metadata.getTenantField().getColumnName();
            sql.append(hasWhere ? " AND " : " WHERE ").append(tenantColumn).append(" = ?");
            hasWhere = true;
        }

        // 数据权限过滤
        if (!dataScopes.isEmpty()) {
            var contextProvider = parentProxy.dataManager.getDataScopeContextProvider();
            if (contextProvider != null) {
                DataScopeSqlBuilder scopeBuilder = new DataScopeSqlBuilder(contextProvider);
                DataScopeSqlBuilder.SqlResult result = scopeBuilder.buildSql(dataScopes, dialect);
                if (result != null) {
                    sql.append(hasWhere ? " AND " : " WHERE ").append(result.sql());
                    hasWhere = true;
                }
            }
        }
    }

    private List<Object> collectParams() {
        List<Object> params = new ArrayList<>();
        if (whereCondition != null && !whereCondition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
            params.addAll(converter.convert(whereCondition).parameters());
        }

        // 租户参数
        String effectiveTenantId = resolveEffectiveTenantId();
        if (effectiveTenantId != null && metadata.getTenantField() != null) {
            params.add(effectiveTenantId);
        }

        // 数据权限参数
        if (!dataScopes.isEmpty()) {
            var contextProvider = parentProxy.dataManager.getDataScopeContextProvider();
            if (contextProvider != null) {
                DataScopeSqlBuilder scopeBuilder = new DataScopeSqlBuilder(contextProvider);
                DataScopeSqlBuilder.SqlResult result = scopeBuilder.buildSql(dataScopes, dialect);
                if (result != null) {
                    params.addAll(result.parameters());
                }
            }
        }

        if (havingCondition != null && !havingCondition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
            params.addAll(converter.convert(havingCondition).parameters());
        }
        return params;
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析有效的租户ID
     * <p>
     * 优先使用显式设置的 tenantId，其次使用 TenantContextHolder 中的上下文租户ID
     */
    private @Nullable String resolveEffectiveTenantId() {
        if (tenantId != null) {
            return tenantId;
        }
        return parentProxy.dataManager.getTenantContextHolder().getTenantId();
    }

    /**
     * 将 Java 字段名解析为数据库列名
     */
    private String resolveColumnName(String fieldName) {
        var fieldMetadata = metadata.getField(fieldName);
        if (fieldMetadata != null) {
            return fieldMetadata.getColumnName();
        }
        // 尝试作为列名直接使用
        for (var fm : metadata.getFields()) {
            if (fm.getColumnName().equals(fieldName)) {
                return fieldName;
            }
        }
        return fieldName;
    }

    // ==================== RowMapper ====================

    /**
     * 聚合结果 RowMapper
     */
    static class AggregateResultRowMapper implements RowMapper<AggregateResult> {

        @Override
        public AggregateResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            ResultSetMetaData metaData = rs.getMetaData();
            Map<String, Object> data = new LinkedHashMap<>();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnLabel = metaData.getColumnLabel(i);
                if (columnLabel == null || columnLabel.isEmpty()) {
                    columnLabel = metaData.getColumnName(i);
                }
                Object value = rs.getObject(i);
                data.put(columnLabel, value);
            }
            return new AggregateResult(data);
        }
    }
}
