package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.mapper.Projection;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.ProjectedQuery;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.data.jdbc.mapper.DtoMapper;
import io.github.afgprojects.framework.data.jdbc.mapper.ResultMapperAdapter;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import io.github.afgprojects.framework.data.sql.scope.DataScopeSqlBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.*;

/**
 * JDBC ProjectedQuery 实现
 * <p>
 * 支持将实体查询结果投影到 DTO 类型（Record 或 POJO）。
 * 当指定 Projection 时，先查询实体再内存映射；否则直接从 ResultSet 映射到 DTO。
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class JdbcProjectedQuery<T, R> implements ProjectedQuery<T, R> {

    private final JdbcEntityQuery<T> entityQuery;
    private final Class<R> dtoType;
    private final @Nullable Projection<T, R> projection;
    private final TypeHandlerRegistry typeHandlerRegistry;

    private Condition condition = Condition.empty();
    private Sort sort;
    private List<DataScope> dataScopes = new ArrayList<>();
    private String tenantId;
    private boolean includeDeleted = false;
    private Integer limit;
    private Integer offset;
    private List<String> selectedFields;

    public JdbcProjectedQuery(JdbcEntityQuery<T> entityQuery, Class<R> dtoType,
                              @Nullable Projection<T, R> projection,
                              TypeHandlerRegistry typeHandlerRegistry) {
        this.entityQuery = entityQuery;
        this.dtoType = dtoType;
        this.projection = projection;
        this.typeHandlerRegistry = typeHandlerRegistry;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> select(@NonNull SFunction<T, ?>... getters) {
        String[] fieldNames = new String[getters.length];
        for (int i = 0; i < getters.length; i++) {
            fieldNames[i] = io.github.afgprojects.framework.data.core.condition.Conditions.getFieldName(getters[i]);
        }
        entityQuery.select(fieldNames);
        this.selectedFields = new ArrayList<>(Arrays.asList(fieldNames));
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> select(@NonNull String... fields) {
        entityQuery.select(fields);
        this.selectedFields = new ArrayList<>(Arrays.asList(fields));
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> where(@NonNull Condition condition) {
        this.condition = condition;
        entityQuery.where(condition);
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> and(@NonNull Condition condition) {
        if (this.condition.isEmpty()) {
            this.condition = condition;
        } else {
            this.condition = this.condition.and(condition);
        }
        entityQuery.and(condition);
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> or(@NonNull Condition condition) {
        if (this.condition.isEmpty()) {
            this.condition = condition;
        } else {
            this.condition = this.condition.or(condition);
        }
        entityQuery.or(condition);
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> orderBy(@NonNull Sort sort) {
        this.sort = sort;
        entityQuery.orderBy(sort);
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> withDataScope() {
        entityQuery.withDataScope();
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> withDataScope(@NonNull String deptField) {
        entityQuery.withDataScope(deptField);
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> withDataScope(@NonNull DataScopeType scopeType) {
        entityQuery.withDataScope(scopeType);
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> withTenant(@NonNull String tenantId) {
        this.tenantId = tenantId;
        entityQuery.withTenant(tenantId);
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> includeDeleted() {
        this.includeDeleted = true;
        entityQuery.includeDeleted();
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> limit(int limit) {
        this.limit = limit;
        entityQuery.limit(limit);
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> offset(int offset) {
        this.offset = offset;
        entityQuery.offset(offset);
        return this;
    }

    @Override
    public @NonNull ProjectedQuery<T, R> withPessimisticLock() {
        entityQuery.withPessimisticLock();
        return this;
    }

    // ==================== 执行方法 ====================

    @Override
    public @NonNull List<R> list() {
        if (projection != null) {
            List<T> entities = entityQuery.list();
            return entities.stream().map(projection::map).toList();
        }
        return executeWithDtoMapper();
    }

    @Override
    public @NonNull Optional<R> one() {
        if (projection != null) {
            return entityQuery.one().map(projection::map);
        }
        entityQuery.limit(2);
        List<R> results = executeWithDtoMapper();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Expected one result but got " + results.size());
        }
        return Optional.of(results.get(0));
    }

    @Override
    public @NonNull Optional<R> first() {
        if (projection != null) {
            return entityQuery.first().map(projection::map);
        }
        entityQuery.limit(1);
        List<R> results = executeWithDtoMapper();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public @NonNull PageData<R> page(@NonNull PageRequest pageRequest) {
        if (projection != null) {
            PageData<T> entityPage = entityQuery.page(pageRequest);
            List<R> mapped = entityPage.records().stream().map(projection::map).toList();
            return PageData.of(mapped, entityPage.total(), entityPage.page(), entityPage.size());
        }
        return executePageWithDtoMapper(pageRequest);
    }

    @Override
    public long count() {
        return entityQuery.count();
    }

    // ==================== 内部方法 ====================

    /**
     * WHERE 子句构建结果
     */
    private record WhereClause(String whereSql, List<Object> parameters) {
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
     */
    private WhereClause buildWhereClause(JdbcEntityProxy<T> proxy, EntityMetadata<T> metadata, Dialect dialect) {
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
            String filter = proxy.getSoftDeleteHandler().getSoftDeleteFilterCondition();
            if (filter != null) {
                if (whereSql.length() > 0) {
                    whereSql.append(" AND ");
                }
                whereSql.append(filter);
            }
        }

        // 3. 租户过滤
        String effectiveTenantId = resolveEffectiveTenantId(proxy);
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
            var contextProvider = proxy.dataManager.getDataScopeContextProvider();
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
    private @Nullable String resolveEffectiveTenantId(JdbcEntityProxy<T> proxy) {
        if (tenantId != null) {
            return tenantId;
        }
        return proxy.dataManager.getTenantContextHolder().getTenantId();
    }

    private List<R> executeWithDtoMapper() {
        JdbcEntityProxy<T> proxy = entityQuery.getParentProxy();
        JdbcClient jdbcClient = proxy.getJdbcClient();
        Dialect dialect = proxy.getDialect();
        EntityMetadata<T> metadata = proxy.dataManager.getEntityMetadata(entityQuery.getEntityClass());

        String sql = buildSelectSql(dialect, metadata);
        WhereClause where = buildWhereClause(proxy, metadata, dialect);
        sql += where.withKeyword();
        List<Object> params = where.jdbcParameters();

        if (sort != null && sort.isSorted()) {
            sql += " ORDER BY " + buildOrderByClause(dialect, metadata);
        }

        // 分页限制（使用 Dialect 生成兼容 SQL）
        if (limit != null) {
            sql = dialect.getPaginationSql(sql, offset != null ? offset : 0, limit);
        }

        DtoMapper<R> dtoMapper = new DtoMapper<>(dtoType, typeHandlerRegistry);
        return jdbcClient.sql(sql)
                .params(params)
                .query(ResultMapperAdapter.adapt(dtoMapper))
                .list();
    }

    private PageData<R> executePageWithDtoMapper(PageRequest pageRequest) {
        JdbcEntityProxy<T> proxy = entityQuery.getParentProxy();
        Dialect dialect = proxy.getDialect();
        EntityMetadata<T> metadata = proxy.dataManager.getEntityMetadata(entityQuery.getEntityClass());

        String baseSql = buildSelectSql(dialect, metadata);
        WhereClause where = buildWhereClause(proxy, metadata, dialect);
        String whereSql = where.withKeyword();
        List<Object> params = where.jdbcParameters();

        String countSql = "SELECT COUNT(*) FROM " + dialect.quoteIdentifier(metadata.getTableName()) + whereSql;
        long total = proxy.getJdbcClient().sql(countSql)
                .params(params)
                .query(Long.class)
                .single();

        String dataSql = baseSql + whereSql;
        if (pageRequest.hasSort()) {
            dataSql += " ORDER BY " + buildOrderByFromPageRequest(dialect, metadata, pageRequest);
        }
        dataSql = dialect.getPaginationSql(dataSql, pageRequest.offset(), pageRequest.size());

        DtoMapper<R> dtoMapper = new DtoMapper<>(dtoType, typeHandlerRegistry);
        List<R> records = proxy.getJdbcClient().sql(dataSql)
                .params(params)
                .query(ResultMapperAdapter.adapt(dtoMapper))
                .list();

        return PageData.of(records, total, pageRequest.page(), pageRequest.size());
    }

    private String buildSelectSql(Dialect dialect, EntityMetadata<T> metadata) {
        if (selectedFields != null && !selectedFields.isEmpty()) {
            // 使用 selectedFields 构建列列表，减少数据库带宽
            StringBuilder columns = new StringBuilder();
            for (int i = 0; i < selectedFields.size(); i++) {
                if (i > 0) {
                    columns.append(", ");
                }
                String fieldName = selectedFields.get(i);
                var fieldMetadata = metadata.getField(fieldName);
                String columnName = fieldMetadata != null ? fieldMetadata.getColumnName() : fieldName;
                columns.append(dialect.quoteIdentifier(columnName));
            }
            return "SELECT " + columns + " FROM " + dialect.quoteIdentifier(metadata.getTableName());
        }
        return "SELECT * FROM " + dialect.quoteIdentifier(metadata.getTableName());
    }

    private String buildOrderByClause(Dialect dialect, EntityMetadata<T> metadata) {
        return OrderByHelper.buildOrderByClause(sort, dialect, metadata);
    }

    private String buildOrderByFromPageRequest(Dialect dialect, EntityMetadata<T> metadata, PageRequest pageRequest) {
        return OrderByHelper.buildOrderByFromPageRequest(pageRequest, dialect, metadata);
    }
}
