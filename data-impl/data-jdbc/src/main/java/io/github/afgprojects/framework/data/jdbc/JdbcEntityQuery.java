package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityQuery;
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
        // 将已有的查询条件传递给投影查询
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
        return pq;
    }

    @Override
    public <R> @NonNull ProjectedQuery<T, R> project(@NonNull Projection<T, R> projection) {
        JdbcProjectedQuery<T, R> pq = new JdbcProjectedQuery<>(this, projection.resultType(), projection, typeHandlerRegistry);
        // 将已有的查询条件传递给投影查询
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
        return pq;
    }

    @Override
    public @NonNull EntityQuery<T> where(@NonNull Condition condition) {
        this.condition = condition;
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

    @Override
    public @NonNull EntityQuery<T> withDataScope() {
        // TODO: 自动检测实体中的部门字段并应用数据权限
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> withDataScope(String deptField) {
        // TODO: 使用指定部门字段应用数据权限
        return this;
    }

    @Override
    public @NonNull EntityQuery<T> withDataScope(DataScopeType scopeType) {
        // TODO: 使用指定数据范围类型应用数据权限
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
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
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
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
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
        dataSql = dialect.getPaginationSql(dataSql, pageRequest.offset(), pageRequest.size());

        List<T> records = jdbcClient.sql(dataSql)
                .params(params)
                .query(rowMapper)
                .list();

        return new Page<>(records, total, pageRequest.page(), pageRequest.size());
    }

    @Override
    public @NonNull Optional<T> one() {
        // 使用 LIMIT 2 而非获取全部结果，节省带宽和查询时间
        String sql = buildSelectSql();
        List<Object> params = new ArrayList<>();

        if (!condition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);
            sql += " WHERE " + result.sql();
            params.addAll(result.parameters());
        }

        // 软删除过滤
        if (!includeDeleted && parentProxy.isSoftDeletable()) {
            sql = appendSoftDeleteFilter(sql, !condition.isEmpty());
        }

        sql += " LIMIT 2";

        List<T> results = jdbcClient.sql(sql)
                .params(params)
                .query(rowMapper)
                .list();

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
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
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
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
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
        // 使用 SELECT 1 ... LIMIT 1 代替 count()，避免全表计数
        String sql = "SELECT 1 FROM " + dialect.quoteIdentifier(metadata.getTableName());
        List<Object> params = new ArrayList<>();

        if (!condition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);
            sql += " WHERE " + result.sql();
            params.addAll(result.parameters());
        }

        // 软删除过滤
        if (!includeDeleted && parentProxy.isSoftDeletable()) {
            sql = appendSoftDeleteFilter(sql, !condition.isEmpty());
        }

        sql += " LIMIT 1";

        List<Integer> results = jdbcClient.sql(sql)
                .params(params)
                .query(Integer.class)
                .list();

        return !results.isEmpty();
    }

    // ==================== 辅助方法 ====================

    private String buildSelectSql() {
        List<String> fields = resolveFields();
        if (fields == null || fields.isEmpty()) {
            return "SELECT * FROM " + dialect.quoteIdentifier(metadata.getTableName());
        }
        // 确保所有字段名被引用（selectedFields 已在 select() 中引用，excludedFields 场景需要在此引用）
        List<String> quotedFields = fields.stream()
                .map(f -> f.startsWith(dialect.getIdentifierQuote()) ? f : dialect.quoteIdentifier(f))
                .toList();
        return "SELECT " + String.join(", ", quotedFields) + " FROM " + dialect.quoteIdentifier(metadata.getTableName());
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
        if (sort == null || !sort.isSorted()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Sort.Order order : sort.getOrders()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            // 将字段名转换为数据库列名
            String fieldName = order.getProperty();
            var fieldMetadata = metadata.getField(fieldName);
            String columnName = fieldMetadata != null ? fieldMetadata.getColumnName() : fieldName;
            sb.append(dialect.quoteIdentifier(columnName));
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
            // 将字段名转换为数据库列名
            String fieldName = order.getProperty();
            var fieldMetadata = metadata.getField(fieldName);
            String columnName = fieldMetadata != null ? fieldMetadata.getColumnName() : fieldName;
            sb.append(dialect.quoteIdentifier(columnName));
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

    Class<T> getEntityClass() {
        return entityClass;
    }

    JdbcEntityProxy<T> getParentProxy() {
        return parentProxy;
    }
}