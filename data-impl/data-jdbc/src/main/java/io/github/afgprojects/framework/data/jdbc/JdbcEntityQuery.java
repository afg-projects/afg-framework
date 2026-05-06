package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityQuery;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleEntityMetadata;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.*;

/**
 * JDBC EntityQuery 实现
 * <p>
 * 提供基于条件的查询操作，支持分页、排序、数据权限等企业级特性。
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class JdbcEntityQuery<T> implements EntityQuery<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    private final JdbcDataManager dataManager;
    private final SimpleEntityMetadata<T> metadata;
    private final RowMapper<T> rowMapper;
    private final JdbcEntityProxy<T> parentProxy;

    private Condition condition = Condition.empty();
    private Sort sort;
    private List<DataScope> dataScopes = new ArrayList<>();
    private String tenantId;
    private String dataSourceName;
    private boolean readOnly = false;
    private boolean includeDeleted = false;
    private final Set<String> eagerFetchAssociations = new LinkedHashSet<>();
    private Integer limit;
    private Integer offset;

    public JdbcEntityQuery(JdbcEntityProxy<T> parentProxy) {
        this.parentProxy = parentProxy;
        this.entityClass = parentProxy.getEntityClass();
        this.jdbcClient = parentProxy.getJdbcClient();
        this.dialect = parentProxy.getDialect();
        this.dataManager = parentProxy.dataManager;
        this.metadata = new SimpleEntityMetadata<>(entityClass);
        this.rowMapper = parentProxy.getRowMapper();
    }

    @Override
    public @NonNull EntityQuery<T> where(@NonNull Condition condition) {
        this.condition = condition;
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> orderBy(@NonNull Sort sort) {
        this.sort = sort;
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

    @Override
    public @NonNull List<T> list() {
        String sql = buildSelectSql();
        List<Object> params = new ArrayList<>();

        if (!condition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);
            sql += " WHERE " + result.sql();
            params.addAll(result.parameters());
        }

        // 软删除过滤
        if (!includeDeleted && parentProxy.isSoftDeletable()) {
            sql = appendSoftDeleteFilter(sql, !condition.isEmpty());
        }

        // 排序
        if (sort != null && sort.isSorted()) {
            sql += " ORDER BY " + buildOrderByClause();
        }

        // 分页限制
        if (limit != null) {
            sql += " LIMIT " + limit;
            if (offset != null) {
                sql += " OFFSET " + offset;
            }
        }

        return jdbcClient.sql(sql)
                .params(params)
                .query(rowMapper)
                .list();
    }

    @Override
    public @NonNull Page<T> page(@NonNull PageRequest pageRequest) {
        // 构建基础查询
        String baseSql = buildSelectSql();
        String whereClause = "";
        List<Object> params = new ArrayList<>();

        if (!condition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);
            whereClause = result.sql();
            params.addAll(result.parameters());
        }

        // 软删除过滤
        if (!includeDeleted && parentProxy.isSoftDeletable()) {
            if (!whereClause.isEmpty()) {
                whereClause += " AND ";
            }
            whereClause += parentProxy.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP
                    ? "deleted_at IS NULL"
                    : "deleted = false";
        }

        String whereSql = !whereClause.isEmpty() ? " WHERE " + whereClause : "";

        // 计数查询
        String countSql = "SELECT COUNT(*) FROM " + dialect.quoteIdentifier(metadata.getTableName()) + whereSql;
        long total = dataManager.queryForCount(countSql, params);

        // 数据查询
        String dataSql = baseSql + whereSql;
        if (pageRequest.hasSort()) {
            dataSql += " ORDER BY " + buildOrderByClauseFromPageRequest(pageRequest);
        }
        dataSql += " LIMIT " + pageRequest.size() + " OFFSET " + pageRequest.offset();

        List<T> records = jdbcClient.sql(dataSql)
                .params(params)
                .query(rowMapper)
                .list();

        return new Page<>(records, total, pageRequest.page(), pageRequest.size());
    }

    @Override
    public @NonNull Optional<T> one() {
        List<T> results = list();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Expected one result but got " + results.size());
        }
        return Optional.of(results.get(0));
    }

    @Override
    public @NonNull Optional<T> first() {
        String sql = buildSelectSql();
        List<Object> params = new ArrayList<>();

        if (!condition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);
            sql += " WHERE " + result.sql();
            params.addAll(result.parameters());
        }

        // 软删除过滤
        if (!includeDeleted && parentProxy.isSoftDeletable()) {
            sql = appendSoftDeleteFilter(sql, !condition.isEmpty());
        }

        sql += " LIMIT 1";

        return jdbcClient.sql(sql)
                .params(params)
                .query(rowMapper)
                .optional();
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + dialect.quoteIdentifier(metadata.getTableName());
        List<Object> params = new ArrayList<>();

        if (!condition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);
            sql += " WHERE " + result.sql();
            params.addAll(result.parameters());
        }

        // 软删除过滤
        if (!includeDeleted && parentProxy.isSoftDeletable()) {
            sql = appendSoftDeleteFilter(sql, !condition.isEmpty());
        }

        Long result = jdbcClient.sql(sql)
                .params(params)
                .query(Long.class)
                .single();
        return result != null ? result : 0L;
    }

    @Override
    public boolean exists() {
        return count() > 0;
    }

    // ==================== 辅助方法 ====================

    private String buildSelectSql() {
        return "SELECT * FROM " + dialect.quoteIdentifier(metadata.getTableName());
    }

    private String buildOrderByClause() {
        if (sort == null || !sort.isSorted()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Sort.Order order : sort.getOrders()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(dialect.quoteIdentifier(order.getProperty()));
            if (order.isDescending()) {
                sb.append(" DESC");
            }
        }
        return sb.toString();
    }

    private String buildOrderByClauseFromPageRequest(PageRequest pageRequest) {
        if (pageRequest.sort() == null || !pageRequest.sort().isSorted()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Sort.Order order : pageRequest.sort().getOrders()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(dialect.quoteIdentifier(order.getProperty()));
            if (order.isDescending()) {
                sb.append(" DESC");
            }
        }
        return sb.toString();
    }

    private String appendSoftDeleteFilter(String sql, boolean hasWhere) {
        String filter = parentProxy.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP
                ? "deleted_at IS NULL"
                : "deleted = false";
        return sql + (hasWhere ? " AND " : " WHERE ") + filter;
    }

    private void validateAssociation(String name) {
        if (!metadata.hasRelation(name)) {
            throw new IllegalArgumentException(
                    "Association '" + name + "' not found in entity " + entityClass.getSimpleName()
            );
        }
    }
}