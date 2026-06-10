package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体条件查询处理器
 * <p>
 * 封装条件查询相关的逻辑，包括条件查询、分页查询等操作。
 * 从 JdbcEntityProxy 中提取，降低类的复杂度。
 * <p>
 * 通过 {@link ProxyStateProvider} 直接读取 proxy 的状态，
 * 避免每次调用前手动同步。
 *
 * @param <T> 实体类型
 */
public class EntityConditionQueryHandler<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;
    private final RowMapper<T> rowMapper;
    private final JdbcDataManager dataManager;
    private final EntitySoftDeleteHandler<T> softDeleteHandler;

    /**
     * Proxy 状态提供者，用于读取 includeDeleted 和 tenantId 状态
     */
    private final ProxyStateProvider stateProvider;

    public EntityConditionQueryHandler(Class<T> entityClass, JdbcClient jdbcClient, Dialect dialect,
                                       EntityMetadata<T> metadata, RowMapper<T> rowMapper,
                                       JdbcDataManager dataManager, EntitySoftDeleteHandler<T> softDeleteHandler,
                                       ProxyStateProvider stateProvider) {
        this.entityClass = entityClass;
        this.jdbcClient = jdbcClient;
        this.dialect = dialect;
        this.metadata = metadata;
        this.rowMapper = rowMapper;
        this.dataManager = dataManager;
        this.softDeleteHandler = softDeleteHandler;
        this.stateProvider = stateProvider;
    }

    /**
     * 根据条件查询实体列表
     *
     * @param condition 查询条件
     * @return 实体列表
     */
    public @NonNull List<T> findAll(@NonNull Condition condition) {
        ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
        ConditionToSqlConverter.SqlResult result = converter.convert(condition);

        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()));

        // 构建 WHERE 子句
        StringBuilder whereClause = new StringBuilder(result.sql());
        List<Object> params = new ArrayList<>(result.parameters());

        // 自动过滤已删除记录
        appendSoftDeleteFilter(whereClause);

        // 租户过滤
        String tenantId = stateProvider.resolveEffectiveTenantId();
        if (tenantId != null && metadata.getTenantField() != null) {
            String tenantColumn = metadata.getTenantField().getColumnName();
            if (whereClause.length() > 0) {
                whereClause.append(" AND ").append(tenantColumn).append(" = ?");
            } else {
                whereClause.append(tenantColumn).append(" = ?");
            }
            params.add(tenantId);
        }

        if (whereClause.length() > 0) {
            sqlBuilder.append(" WHERE ").append(whereClause);
        }

        // 转换条件参数中的 Java 时间类型为 JDBC 兼容类型
        List<Object> convertedParams = JdbcTypeConverter.convertParamsForJdbc(params);

        return jdbcClient.sql(sqlBuilder.toString())
                .params(convertedParams)
                .query(rowMapper)
                .list();
    }

    /**
     * 根据条件分页查询实体
     *
     * @param condition 查询条件
     * @param pageable  分页参数
     * @return 分页结果
     */
    public @NonNull Page<T> findAll(@NonNull Condition condition, @NonNull PageRequest pageable) {
        ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
        ConditionToSqlConverter.SqlResult whereResult = converter.convert(condition);

        // 构建基础 WHERE 子句
        StringBuilder whereClause = new StringBuilder(whereResult.sql());
        List<Object> params = new ArrayList<>(whereResult.parameters());

        // 自动过滤已删除记录
        appendSoftDeleteFilter(whereClause);

        // 租户过滤
        String tenantId = stateProvider.resolveEffectiveTenantId();
        if (tenantId != null && metadata.getTenantField() != null) {
            String tenantColumn = metadata.getTenantField().getColumnName();
            if (whereClause.length() > 0) {
                whereClause.append(" AND ").append(tenantColumn).append(" = ?");
            } else {
                whereClause.append(tenantColumn).append(" = ?");
            }
            params.add(tenantId);
        }

        // 构建完整 SQL
        String whereSql = whereClause.length() > 0 ? " WHERE " + whereClause : "";

        // 转换条件参数中的 Java 时间类型为 JDBC 兼容类型
        List<Object> convertedParams = JdbcTypeConverter.convertParamsForJdbc(params);

        // 计数查询
        String countSql = "SELECT COUNT(*) FROM " + dialect.quoteIdentifier(metadata.getTableName()) + whereSql;
        long total = dataManager.queryForCount(countSql, convertedParams);

        // 数据查询（使用 Dialect 生成兼容的分页 SQL）
        String dataSql = "SELECT * FROM " + dialect.quoteIdentifier(metadata.getTableName()) +
                whereSql;
        dataSql = dialect.getPaginationSql(dataSql, pageable.offset(), pageable.size());
        List<T> records = jdbcClient.sql(dataSql)
                .params(convertedParams)
                .query(rowMapper)
                .list();

        return new Page<>(records, total, pageable.page(), pageable.size());
    }

    /**
     * 追加软删除过滤条件
     */
    private void appendSoftDeleteFilter(StringBuilder whereClause) {
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() == null || includeDeleted) {
            return;
        }

        String filter = softDeleteHandler.getSoftDeleteFilterCondition();
        if (filter == null) {
            return;
        }

        if (whereClause.length() > 0) {
            whereClause.append(" AND ").append(filter);
        } else {
            whereClause.append(filter);
        }
    }
}
