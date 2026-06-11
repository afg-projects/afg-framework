package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.data.core.EntityQuery;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.mapper.Projection;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.ProjectedQuery;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.query.AggregateQuery;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import io.github.afgprojects.framework.data.sql.scope.DataScopeSqlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.*;

/**
 * JDBC EntityQuery 实现
 * <p>
 * 提供基于条件的查询操作，支持分页、排序、数据权限、租户过滤等企业级特性。
 * <p>
 * 所有查询方法（list/one/first/count/exists/page）通过统一的 {@link #buildWhereClause()} 构建 WHERE 子句，
 * 确保软删除过滤、租户过滤、数据权限过滤在所有查询路径上一致应用。
 */
@Slf4j
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class JdbcEntityQuery<T> implements EntityQuery<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    private final JdbcDataManager dataManager;
    private final EntityMetadata<T> metadata;
    private final RowMapper<T> rowMapper;
    private final JdbcEntityProxy<T> parentProxy;
    private final TypeHandlerRegistry typeHandlerRegistry;

    private Condition condition = Condition.empty();
    private Sort sort;
    private List<DataScope> dataScopes = new ArrayList<>();
    private String tenantId;
    private String dataSourceName;
    private boolean readOnly = false;
    private boolean includeDeleted = false;
    private boolean distinct = false;
    private final Set<String> eagerFetchAssociations = new LinkedHashSet<>();
    private Integer limit;
    private Integer offset;

    /**
     * 选中的字段列表（null 表示查询所有字段）
     */
    private List<String> selectedFields;

    /**
     * 排除的字段列表
     */
    private Set<String> excludedFields = new HashSet<>();

    public JdbcEntityQuery(JdbcEntityProxy<T> parentProxy) {
        this.parentProxy = parentProxy;
        this.entityClass = parentProxy.getEntityClass();
        this.jdbcClient = parentProxy.getJdbcClient();
        this.dialect = parentProxy.getDialect();
        this.dataManager = parentProxy.dataManager;
        this.metadata = dataManager.getEntityMetadata(entityClass);
        this.rowMapper = parentProxy.getRowMapper();
        this.typeHandlerRegistry = parentProxy.getTypeHandlerRegistry();
    }

    // ==================== DTO 投影 ====================

    @Override
    public <R> @NonNull ProjectedQuery<T, R> project(@NonNull Class<R> dtoType) {
        JdbcProjectedQuery<T, R> pq = new JdbcProjectedQuery<>(this, dtoType, null, typeHandlerRegistry);
        transferQueryStateTo(pq);
        return pq;
    }

    @Override
    public <R> @NonNull ProjectedQuery<T, R> project(@NonNull Projection<T, R> projection) {
        JdbcProjectedQuery<T, R> pq = new JdbcProjectedQuery<>(this, projection.resultType(), projection, typeHandlerRegistry);
        transferQueryStateTo(pq);
        return pq;
    }

    // ==================== 条件配置 ====================

    @Override
    public @NonNull EntityQuery<T> where(@NonNull Condition condition) {
        this.condition = condition;
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> and(@NonNull Condition condition) {
        if (this.condition.isEmpty()) {
            this.condition = condition;
        } else {
            this.condition = this.condition.and(condition);
        }
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> or(@NonNull Condition condition) {
        if (this.condition.isEmpty()) {
            this.condition = condition;
        } else {
            this.condition = this.condition.or(condition);
        }
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> distinct() {
        this.distinct = true;
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> orderBy(@NonNull Sort sort) {
        if (this.sort == null || this.sort.isUnsorted()) {
            this.sort = sort;
        } else {
            this.sort = this.sort.and(sort);
        }
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> select(@NonNull String... fields) {
        // 校验字段名必须在实体元数据中存在，防止 SQL 注入
        List<String> validated = new ArrayList<>(fields.length);
        for (String field : fields) {
            var fieldMetadata = metadata.getField(field);
            if (fieldMetadata != null) {
                validated.add(dialect.quoteIdentifier(fieldMetadata.getColumnName()));
            } else {
                // 尝试作为列名匹配
                boolean found = false;
                for (var fm : metadata.getFields()) {
                    if (fm.getColumnName().equals(field)) {
                        validated.add(dialect.quoteIdentifier(field));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException(
                            "Invalid field '" + field + "' for entity " + entityClass.getSimpleName()
                    );
                }
            }
        }
        this.selectedFields = validated;
        this.excludedFields.clear();
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> exclude(@NonNull String... fields) {
        for (String field : fields) {
            // 校验字段名必须在实体元数据中存在
            if (metadata.getField(field) == null) {
                boolean found = false;
                for (var fm : metadata.getFields()) {
                    if (fm.getColumnName().equals(field)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException(
                            "Invalid field '" + field + "' for entity " + entityClass.getSimpleName()
                    );
                }
            }
            this.excludedFields.add(field);
        }
        this.selectedFields = null;
        return this;
    }

    // ==================== 数据权限配置 ====================

    @Override
    public @NonNull EntityQuery<T> withDataScope() {
        // 自动检测实体中的部门字段
        String deptColumn = findDeptColumn();
        if (deptColumn != null) {
            this.dataScopes.add(DataScope.of(metadata.getTableName(), deptColumn, DataScopeType.DEPT_AND_CHILD));
        }
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> withDataScope(String deptField) {
        String columnName = resolveColumnNameFromFieldName(deptField);
        this.dataScopes.add(DataScope.of(metadata.getTableName(), columnName, DataScopeType.DEPT_AND_CHILD));
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> withDataScope(DataScopeType scopeType) {
        String deptColumn = findDeptColumn();
        if (deptColumn != null) {
            this.dataScopes.add(DataScope.of(metadata.getTableName(), deptColumn, scopeType));
        }
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> withDataScope(@NonNull DataScope scope) {
        this.dataScopes.add(scope);
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> withDataScopes(@NonNull DataScope... scopes) {
        this.dataScopes.addAll(Arrays.asList(scopes));
        return this;
    }

    // ==================== 租户/数据源/只读配置 ====================

    @Override
    public @NonNull EntityQuery<T> withTenant(@NonNull String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> withDataSource(@NonNull String name) {
        this.dataSourceName = name;
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> withReadOnly() {
        this.readOnly = true;
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> includeDeleted() {
        this.includeDeleted = true;
        return this;
    }

    // ==================== 关联加载配置 ====================

    @Override
    public @NonNull EntityQuery<T> withAssociation(@NonNull String name) {
        validateAssociation(name);
        eagerFetchAssociations.add(name);
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> withAssociations(@NonNull String... names) {
        for (String name : names) {
            validateAssociation(name);
            eagerFetchAssociations.add(name);
        }
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> clearAssociations() {
        eagerFetchAssociations.clear();
        return this;
    }

    /**
     * 获取急加载关联配置（用于测试）
     *
     * @return 关联字段名集合
     */
    Set<String> getEagerFetchAssociations() {
        return Collections.unmodifiableSet(eagerFetchAssociations);
    }

    // ==================== 分页限制配置 ====================

    @Override
    public @NonNull EntityQuery<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> offset(int offset) {
        this.offset = offset;
        return this;
    }

    // ==================== 查询执行方法 ====================

    @Override
    public @NonNull List<T> list() {
        String selectSql = buildSelectSql();
        WhereClause where = buildWhereClause();
        String sql = selectSql + where.withKeyword();
        List<Object> params = where.jdbcParameters();

        // 排序
        if (sort != null && sort.isSorted()) {
            sql += " ORDER BY " + buildOrderByClause();
        }

        // 分页限制
        if (limit != null) {
            sql = dialect.getPaginationSql(sql, offset != null ? offset : 0, limit);
        }

        List<T> results = executeQuery(sql, params);

        // 急加载关联
        results = loadAssociations(results);

        return results;
    }

    @Override
    public @NonNull PageData<T> page(@NonNull PageRequest pageRequest) {
        WhereClause where = buildWhereClause();
        String whereSql = where.withKeyword();
        List<Object> params = where.jdbcParameters();

        // 计数查询
        String countSql = "SELECT COUNT(*) FROM " + dialect.quoteIdentifier(metadata.getTableName()) + whereSql;
        long total = dataManager.queryForCount(countSql, params);

        // 数据查询
        String dataSql = buildSelectSql() + whereSql;
        if (pageRequest.hasSort()) {
            dataSql += " ORDER BY " + buildOrderByClauseFromPageRequest(pageRequest);
        }
        dataSql = dialect.getPaginationSql(dataSql, pageRequest.offset(), pageRequest.size());

        List<T> records = executeQuery(dataSql, params);

        // 急加载关联
        records = loadAssociations(records);

        return PageData.of(records, total, pageRequest.page(), pageRequest.size());
    }

    @Override
    public @NonNull Optional<T> one() {
        String selectSql = buildSelectSql();
        WhereClause where = buildWhereClause();
        String sql = selectSql + where.withKeyword();
        List<Object> params = where.jdbcParameters();

        sql = dialect.getLimitSql(sql, 2);

        List<T> results = executeQuery(sql, params);

        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Expected one result but got " + results.size());
        }
        return Optional.of(loadAssociationsSingle(results.get(0)));
    }

    @Override
    public @NonNull Optional<T> first() {
        String selectSql = buildSelectSql();
        WhereClause where = buildWhereClause();
        String sql = selectSql + where.withKeyword();
        List<Object> params = where.jdbcParameters();

        sql = dialect.getLimitSql(sql, 1);

        List<T> results = executeQuery(sql, params);
        return results.isEmpty() ? Optional.empty() : Optional.of(loadAssociationsSingle(results.get(0)));
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + dialect.quoteIdentifier(metadata.getTableName());
        WhereClause where = buildWhereClause();
        sql += where.withKeyword();
        List<Object> params = where.jdbcParameters();

        Long result = jdbcClient.sql(sql)
                .params(params)
                .query(Long.class)
                .single();
        return result != null ? result : 0L;
    }

    @Override
    public boolean exists() {
        String sql = "SELECT 1 FROM " + dialect.quoteIdentifier(metadata.getTableName());
        WhereClause where = buildWhereClause();
        sql += where.withKeyword();
        List<Object> params = where.jdbcParameters();

        sql = dialect.getLimitSql(sql, 1);

        List<Integer> results = jdbcClient.sql(sql)
                .params(params)
                .query(Integer.class)
                .list();

        return !results.isEmpty();
    }

    @Override
    public @NonNull AggregateQuery<T> aggregate() {
        return new JdbcAggregateQuery<>(parentProxy);
    }

    // ==================== WHERE 子句统一构建 ====================

    /**
     * WHERE 子句构建结果
     */
    private record WhereClause(String whereSql, List<Object> parameters) {
        static WhereClause empty() { return new WhereClause("", List.of()); }
        boolean hasWhere() { return !whereSql.isEmpty(); }
        String withKeyword() { return hasWhere() ? " WHERE " + whereSql : ""; }

        /**
         * 返回 JDBC 兼容的参数列表（Java 时间类型已转换为 SQL 类型）
         */
        List<Object> jdbcParameters() {
            return JdbcTypeConverter.convertParamsForJdbc(parameters);
        }
    }

    /**
     * 统一构建 WHERE 子句（条件 + 软删除 + 租户过滤 + 数据权限）
     * <p>
     * 所有查询方法（list/one/first/count/exists/page）都应调用此方法，
     * 确保数据权限和租户过滤在所有查询路径上一致应用。
     */
    private WhereClause buildWhereClause() {
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        // 1. 用户条件
        if (!condition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);
            whereSql.append(result.sql());
            params.addAll(result.parameters());
        }

        // 2. 软删除过滤
        if (!includeDeleted && (metadata.hasTrait(EntityTrait.SOFT_DELETABLE) || metadata.hasTrait(EntityTrait.TIMESTAMP_SOFT_DELETABLE))) {
            if (whereSql.length() > 0) {
                whereSql.append(" AND ");
            }
            whereSql.append(parentProxy.getSoftDeleteHandler().getSoftDeleteFilterCondition());
        }

        // 3. 租户过滤
        String effectiveTenantId = resolveEffectiveTenantId();
        if (effectiveTenantId != null && metadata.getTenantField() != null) {
            if (whereSql.length() > 0) {
                whereSql.append(" AND ");
            }
            String tenantColumn = metadata.getTenantField().getColumnName();
            whereSql.append(tenantColumn).append(" = ?");
            params.add(effectiveTenantId);
        }

        // 4. 数据权限过滤
        if (!dataScopes.isEmpty()) {
            var contextProvider = parentProxy.dataManager.getDataScopeContextProvider();
            if (contextProvider != null) {
                DataScopeSqlBuilder scopeBuilder = new DataScopeSqlBuilder(contextProvider);
                DataScopeSqlBuilder.SqlResult result = scopeBuilder.buildSql(dataScopes, dialect);
                if (result != null) {
                    if (whereSql.length() > 0) {
                        whereSql.append(" AND ");
                    }
                    whereSql.append(result.sql());
                    params.addAll(result.parameters());
                }
            }
        }

        return new WhereClause(whereSql.toString(), params);
    }

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

    // ==================== 查询执行辅助方法 ====================

    /**
     * 执行查询（支持只读模式）
     */
    private List<T> executeQuery(String sql, List<Object> params) {
        if (readOnly) {
            return dataManager.executeInReadOnly(() ->
                jdbcClient.sql(sql).params(params).query(rowMapper).list()
            );
        }
        return jdbcClient.sql(sql).params(params).query(rowMapper).list();
    }

    /**
     * 急加载关联数据（列表）
     */
    private List<T> loadAssociations(List<T> results) {
        if (!eagerFetchAssociations.isEmpty() && !results.isEmpty()) {
            for (String association : eagerFetchAssociations) {
                parentProxy.fetchAll(results, association);
            }
        }
        return results;
    }

    /**
     * 急加载关联数据（单个实体）
     */
    private T loadAssociationsSingle(T entity) {
        if (!eagerFetchAssociations.isEmpty()) {
            for (String association : eagerFetchAssociations) {
                parentProxy.fetchAll(List.of(entity), association);
            }
        }
        return entity;
    }

    // ==================== 投影状态传递 ====================

    /**
     * 将当前查询状态传递给投影查询
     */
    private <R> void transferQueryStateTo(JdbcProjectedQuery<T, R> pq) {
        if (!condition.isEmpty()) {
            pq.where(condition);
        }
        if (sort != null && sort.isSorted()) {
            pq.orderBy(sort);
        }
        if (tenantId != null) {
            pq.withTenant(tenantId);
        }
        if (includeDeleted) {
            pq.includeDeleted();
        }
        if (limit != null) {
            pq.limit(limit);
        }
        if (offset != null) {
            pq.offset(offset);
        }
    }

    // ==================== SQL 构建辅助方法 ====================

    private String buildSelectSql() {
        List<String> fields = resolveFields();
        String prefix = distinct ? "SELECT DISTINCT " : "SELECT ";
        if (fields == null || fields.isEmpty()) {
            return prefix + "* FROM " + dialect.quoteIdentifier(metadata.getTableName());
        }
        // 确保所有字段名被引用（selectedFields 已在 select() 中引用，excludedFields 场景需要在此引用）
        List<String> quotedFields = fields.stream()
                .map(f -> f.startsWith(dialect.getIdentifierQuote()) ? f : dialect.quoteIdentifier(f))
                .toList();
        return prefix + String.join(", ", quotedFields) + " FROM " + dialect.quoteIdentifier(metadata.getTableName());
    }

    /**
     * 解析要查询的字段列表
     *
     * @return 字段列表，null 表示查询所有字段
     */
    private List<String> resolveFields() {
        if (selectedFields != null && !selectedFields.isEmpty()) {
            return selectedFields;
        }

        if (!excludedFields.isEmpty()) {
            List<String> fields = new ArrayList<>();
            for (var field : metadata.getFields()) {
                String columnName = field.getColumnName();
                String propertyName = field.getPropertyName();
                if (!excludedFields.contains(columnName) && !excludedFields.contains(propertyName)) {
                    fields.add(columnName);
                }
            }
            return fields.isEmpty() ? null : fields;
        }

        return null;
    }

    private String buildOrderByClause() {
        return OrderByHelper.buildOrderByClause(sort, dialect, metadata);
    }

    private String buildOrderByClauseFromPageRequest(PageRequest pageRequest) {
        return OrderByHelper.buildOrderByFromPageRequest(pageRequest, dialect, metadata);
    }

    private void validateAssociation(String name) {
        if (!metadata.hasRelation(name)) {
            throw new IllegalArgumentException(
                    "Association '" + name + "' not found in entity " + entityClass.getSimpleName()
            );
        }
    }

    /**
     * 自动检测实体中的部门字段
     * <p>
     * 按优先级检测常见部门字段名：deptId, dept_id, departmentId, department_id, orgId, org_id
     *
     * @return 列名，未找到返回 null
     */
    private @Nullable String findDeptColumn() {
        for (String candidate : List.of("deptId", "dept_id", "departmentId", "department_id", "orgId", "org_id")) {
            var fieldMetadata = metadata.getField(candidate);
            if (fieldMetadata != null) {
                return fieldMetadata.getColumnName();
            }
            // 尝试作为列名匹配
            for (var fm : metadata.getFields()) {
                if (fm.getColumnName().equals(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * 将 Java 字段名解析为数据库列名
     */
    private String resolveColumnNameFromFieldName(String fieldName) {
        var fieldMetadata = metadata.getField(fieldName);
        if (fieldMetadata != null) {
            return fieldMetadata.getColumnName();
        }
        return fieldName;
    }

    // ==================== 测试辅助方法 ====================

    Class<T> getEntityClass() {
        return entityClass;
    }

    JdbcEntityProxy<T> getParentProxy() {
        return parentProxy;
    }
}
