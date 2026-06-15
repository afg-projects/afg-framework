package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 实体查询执行器
 * <p>
 * 封装实体查询相关的逻辑，包括单条查询、批量查询、计数等操作。
 * 从 JdbcEntityProxy 中提取，降低类的复杂度。
 * <p>
 * 通过 {@link ProxyStateProvider} 直接读取 proxy 的状态，
 * 避免每次调用前手动同步。
 *
 * @param <T> 实体类型
 */
public class EntityQueryExecutor<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;
    private final RowMapper<T> rowMapper;
    private final EntityCacheHandler<T> cacheHandler;
    private final EntitySoftDeleteHandler<T> softDeleteHandler;

    /**
     * Proxy 状态提供者，用于读取 includeDeleted 和 tenantId 状态
     */
    private final ProxyStateProvider stateProvider;

    /**
     * ID 列名（从元数据获取）
     */
    private final String idColumnName;

    public EntityQueryExecutor(Class<T> entityClass, JdbcClient jdbcClient, Dialect dialect,
                               EntityMetadata<T> metadata, RowMapper<T> rowMapper,
                               EntityCacheHandler<T> cacheHandler, EntitySoftDeleteHandler<T> softDeleteHandler,
                               ProxyStateProvider stateProvider) {
        this.entityClass = entityClass;
        this.jdbcClient = jdbcClient;
        this.dialect = dialect;
        this.metadata = metadata;
        this.rowMapper = rowMapper;
        this.cacheHandler = cacheHandler;
        this.softDeleteHandler = softDeleteHandler;
        this.stateProvider = stateProvider;
        // 从元数据获取 ID 列名，向后兼容默认 "id"
        this.idColumnName = metadata.getIdField() != null ? metadata.getIdField().getColumnName() : "id";
    }

    /**
     * 根据 ID 查询实体
     *
     * @param id 实体 ID
     * @return 实体（可能为空）
     */
    public @NonNull Optional<T> findById(@NonNull Object id) {
        // 尝试从缓存获取
        EntityCacheHandler.CacheResult<T> cacheResult = cacheHandler.get(id);
        if (cacheResult.isHit()) {
            if (cacheResult.isNullHit()) {
                return Optional.empty();
            }
            return cacheResult.getEntity();
        }

        // 从数据库查询
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()))
                .append(" WHERE ").append(dialect.quoteIdentifier(idColumnName)).append(" = :id");

        // 自动过滤已删除记录
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, true, includeDeleted);
        }

        // 租户过滤（已有 WHERE id = :id，直接追加 AND）
        String tenantId = stateProvider.resolveEffectiveTenantId();
        if (tenantId != null && metadata.getTenantField() != null) {
            sqlBuilder.append(" AND ").append(metadata.getTenantField().getColumnName()).append(" = :tenantId");
        }

        // 执行查询
        var querySpec = jdbcClient.sql(sqlBuilder.toString()).param("id", id);
        if (tenantId != null && metadata.getTenantField() != null) {
            querySpec = querySpec.param("tenantId", tenantId);
        }
        Optional<T> result = querySpec.query(rowMapper).optional();

        // 缓存结果
        result.ifPresentOrElse(
                entity -> cacheHandler.put(id, entity),
                () -> cacheHandler.put(id, null)
        );

        return result;
    }

    /**
     * 查询所有实体
     *
     * @return 实体列表
     */
    public @NonNull List<T> findAll() {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()));

        // 追踪是否有 WHERE 子句
        boolean hasWhere = false;

        // 自动过滤已删除记录
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, false, includeDeleted);
            hasWhere = true;
        }

        // 租户过滤
        String tenantId = stateProvider.resolveEffectiveTenantId();
        if (tenantId != null && metadata.getTenantField() != null) {
            if (hasWhere) {
                sqlBuilder.append(" AND ").append(metadata.getTenantField().getColumnName()).append(" = :tenantId");
            } else {
                sqlBuilder.append(" WHERE ").append(metadata.getTenantField().getColumnName()).append(" = :tenantId");
            }
        }

        var querySpec = jdbcClient.sql(sqlBuilder.toString());
        if (tenantId != null && metadata.getTenantField() != null) {
            querySpec = querySpec.param("tenantId", tenantId);
        }
        return querySpec.query(rowMapper).list();
    }

    /**
     * 根据多个 ID 查询实体
     *
     * @param ids ID 集合
     * @return 实体列表
     */
    public @NonNull List<T> findAllById(@NonNull Iterable<?> ids) {
        List<Object> idList = new ArrayList<>();
        ids.forEach(idList::add);
        if (idList.isEmpty()) {
            return List.of();
        }

        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()))
                .append(" WHERE ").append(dialect.quoteIdentifier(idColumnName)).append(" IN (:ids)");

        // 自动过滤已删除记录
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, true, includeDeleted);
        }

        // 租户过滤（已有 WHERE id IN (:ids)，直接追加 AND）
        String tenantId = stateProvider.resolveEffectiveTenantId();
        if (tenantId != null && metadata.getTenantField() != null) {
            sqlBuilder.append(" AND ").append(metadata.getTenantField().getColumnName()).append(" = :tenantId");
        }

        var querySpec = jdbcClient.sql(sqlBuilder.toString()).param("ids", idList);
        if (tenantId != null && metadata.getTenantField() != null) {
            querySpec = querySpec.param("tenantId", tenantId);
        }
        return querySpec.query(rowMapper).list();
    }

    /**
     * 统计实体总数
     *
     * @return 实体总数
     */
    public long count() {
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()));

        // 追踪是否有 WHERE 子句
        boolean hasWhere = false;

        // 自动过滤已删除记录
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, false, includeDeleted);
            hasWhere = true;
        }

        // 租户过滤
        String tenantId = stateProvider.resolveEffectiveTenantId();
        if (tenantId != null && metadata.getTenantField() != null) {
            if (hasWhere) {
                sqlBuilder.append(" AND ").append(metadata.getTenantField().getColumnName()).append(" = :tenantId");
            } else {
                sqlBuilder.append(" WHERE ").append(metadata.getTenantField().getColumnName()).append(" = :tenantId");
            }
        }

        var querySpec = jdbcClient.sql(sqlBuilder.toString());
        if (tenantId != null && metadata.getTenantField() != null) {
            querySpec = querySpec.param("tenantId", tenantId);
        }
        Long result = querySpec.query(Long.class).single();
        return result != null ? result : 0L;
    }

    /**
     * 根据 ID 判断实体是否存在
     * <p>
     * 使用 {@code SELECT 1 ... LIMIT 1} 代替 {@link #findById}，
     * 避免完整的实体映射，提高查询效率。
     *
     * @param id 实体 ID
     * @return 是否存在
     */
    public boolean existsById(@NonNull Object id) {
        // 先检查缓存（如果缓存命中，避免数据库查询）
        EntityCacheHandler.CacheResult<T> cacheResult = cacheHandler.get(id);
        if (cacheResult.isHit()) {
            return !cacheResult.isNullHit();
        }

        // 使用 SELECT 1 代替 SELECT *，避免完整实体映射
        StringBuilder sqlBuilder = new StringBuilder("SELECT 1 FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()))
                .append(" WHERE ").append(dialect.quoteIdentifier(idColumnName)).append(" = :id");

        // 自动过滤已删除记录
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, true, includeDeleted);
        }

        // 租户过滤（已有 WHERE id = :id，直接追加 AND）
        String tenantId = stateProvider.resolveEffectiveTenantId();
        if (tenantId != null && metadata.getTenantField() != null) {
            sqlBuilder.append(" AND ").append(metadata.getTenantField().getColumnName()).append(" = :tenantId");
        }

        // 使用 Dialect 生成分页 SQL（LIMIT 1）
        String sql = dialect.getLimitSql(sqlBuilder.toString(), 1);

        var querySpec = jdbcClient.sql(sql).param("id", id);
        if (tenantId != null && metadata.getTenantField() != null) {
            querySpec = querySpec.param("tenantId", tenantId);
        }
        return querySpec.query(Integer.class).optional().isPresent();
    }

    /**
     * 追加软删除过滤条件
     */
    private void appendSoftDeleteFilter(StringBuilder sqlBuilder, boolean hasWhereClause, boolean includeDeleted) {
        softDeleteHandler.appendSoftDeleteFilter(sqlBuilder, hasWhereClause, includeDeleted);
    }
}
