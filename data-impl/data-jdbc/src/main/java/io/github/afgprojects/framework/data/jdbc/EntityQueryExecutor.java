package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import org.jspecify.annotations.NonNull;
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
     * Proxy 状态提供者，用于读取 includeDeleted 状态
     */
    private final ProxyStateProvider stateProvider;

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
                .append(" WHERE id = :id");

        // 自动过滤已删除记录
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, true, includeDeleted);
        }

        Optional<T> result = jdbcClient.sql(sqlBuilder.toString())
                .param("id", id)
                .query(rowMapper)
                .optional();

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

        // 自动过滤已删除记录
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, false, includeDeleted);
        }

        return jdbcClient.sql(sqlBuilder.toString())
                .query(rowMapper)
                .list();
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
                .append(" WHERE id IN (:ids)");

        // 自动过滤已删除记录
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, true, includeDeleted);
        }

        return jdbcClient.sql(sqlBuilder.toString())
                .param("ids", idList)
                .query(rowMapper)
                .list();
    }

    /**
     * 统计实体总数
     *
     * @return 实体总数
     */
    public long count() {
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()));

        // 自动过滤已删除记录
        boolean includeDeleted = stateProvider.isIncludeDeleted();
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, false, includeDeleted);
        }

        Long result = jdbcClient.sql(sqlBuilder.toString())
                .query(Long.class)
                .single();
        return result != null ? result : 0L;
    }

    /**
     * 根据 ID 判断实体是否存在
     *
     * @param id 实体 ID
     * @return 是否存在
     */
    public boolean existsById(@NonNull Object id) {
        return findById(id).isPresent();
    }

    /**
     * 追加软删除过滤条件
     */
    private void appendSoftDeleteFilter(StringBuilder sqlBuilder, boolean hasWhereClause, boolean includeDeleted) {
        softDeleteHandler.appendSoftDeleteFilter(sqlBuilder, hasWhereClause, includeDeleted);
    }
}
