package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.mapper.Projection;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.query.ProjectedQuery;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.data.jdbc.mapper.DtoMapper;
import io.github.afgprojects.framework.data.jdbc.mapper.ResultMapperAdapter;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
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
    public @NonNull Page<R> page(@NonNull PageRequest pageRequest) {
        if (projection != null) {
            Page<T> entityPage = entityQuery.page(pageRequest);
            List<R> mapped = entityPage.getContent().stream().map(projection::map).toList();
            return new Page<>(mapped, entityPage.getTotal(), entityPage.getPage(), entityPage.getSize());
        }
        return executePageWithDtoMapper(pageRequest);
    }

    @Override
    public long count() {
        return entityQuery.count();
    }

    // ==================== 内部方法 ====================

    private List<R> executeWithDtoMapper() {
        JdbcEntityProxy<T> proxy = entityQuery.getParentProxy();
        JdbcClient jdbcClient = proxy.getJdbcClient();
        Dialect dialect = proxy.getDialect();
        EntityMetadata<T> metadata = proxy.dataManager.getEntityMetadata(entityQuery.getEntityClass());

        String sql = buildSelectSql(dialect, metadata);
        List<Object> params = new ArrayList<>();

        if (!condition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);
            sql += " WHERE " + result.sql();
            params.addAll(result.parameters());
        }

        if (!includeDeleted && proxy.isSoftDeletable()) {
            sql = appendSoftDeleteFilter(sql, !condition.isEmpty(), proxy);
        }

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

    private Page<R> executePageWithDtoMapper(PageRequest pageRequest) {
        JdbcEntityProxy<T> proxy = entityQuery.getParentProxy();
        Dialect dialect = proxy.getDialect();
        EntityMetadata<T> metadata = proxy.dataManager.getEntityMetadata(entityQuery.getEntityClass());

        String baseSql = buildSelectSql(dialect, metadata);
        String whereClause = "";
        List<Object> params = new ArrayList<>();

        if (!condition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);
            whereClause = result.sql();
            params.addAll(result.parameters());
        }

        if (!includeDeleted && proxy.isSoftDeletable()) {
            String filter = proxy.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP
                    ? "deleted_at IS NULL"
                    : "deleted = false";
            if (!whereClause.isEmpty()) {
                whereClause += " AND " + filter;
            } else {
                whereClause += filter;
            }
        }

        String whereSql = !whereClause.isEmpty() ? " WHERE " + whereClause : "";

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

        return new Page<>(records, total, pageRequest.page(), pageRequest.size());
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

    private String appendSoftDeleteFilter(String sql, boolean hasWhere, JdbcEntityProxy<T> proxy) {
        return sql + proxy.getSoftDeleteHandler().buildSoftDeleteFilterSql(hasWhere, includeDeleted);
    }

    private String buildOrderByClause(Dialect dialect, EntityMetadata<T> metadata) {
        return OrderByHelper.buildOrderByClause(sort, dialect, metadata);
    }

    private String buildOrderByFromPageRequest(Dialect dialect, EntityMetadata<T> metadata, PageRequest pageRequest) {
        return OrderByHelper.buildOrderByFromPageRequest(pageRequest, dialect, metadata);
    }
}