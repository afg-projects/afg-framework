package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;

/**
 * 实体删除操作处理器
 * <p>
 * 封装实体删除相关的逻辑，包括物理删除、软删除等操作。
 * 从 JdbcEntityProxy 中提取，降低类的复杂度。
 *
 * @param <T> 实体类型
 */
public class EntityDeleteHandler<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;
    private final EntitySoftDeleteHandler<T> softDeleteHandler;
    private final EntityCacheHandler<T> cacheHandler;
    private final EntityQueryHelper<T> queryHelper;

    public EntityDeleteHandler(Class<T> entityClass, JdbcClient jdbcClient, Dialect dialect,
                               EntityMetadata<T> metadata, EntitySoftDeleteHandler<T> softDeleteHandler,
                               EntityCacheHandler<T> cacheHandler, EntityQueryHelper<T> queryHelper) {
        this.entityClass = entityClass;
        this.jdbcClient = jdbcClient;
        this.dialect = dialect;
        this.metadata = metadata;
        this.softDeleteHandler = softDeleteHandler;
        this.cacheHandler = cacheHandler;
        this.queryHelper = queryHelper;
    }

    /**
     * 根据 ID 删除实体
     * <p>
     * 如果实体支持软删除，执行软删除；否则执行物理删除。
     *
     * @param id 实体 ID
     */
    public void deleteById(@NonNull Object id) {
        // 如果实体支持软删除，执行软删除
        if (softDeleteHandler.isSoftDeletable()) {
            softDeleteById(id);
            return;
        }

        // 物理删除
        String sql = "DELETE FROM " + dialect.quoteIdentifier(metadata.getTableName()) + " WHERE id = :id";
        jdbcClient.sql(sql)
                .param("id", id)
                .update();

        // 失效缓存
        cacheHandler.evict(id);
    }

    /**
     * 软删除指定 ID 的记录
     *
     * @param id 实体 ID
     */
    private void softDeleteById(@NonNull Object id) {
        String sql;
        if (softDeleteHandler.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP) {
            // 时间戳模式：设置 deleted_at 为当前时间
            sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                    " SET deleted_at = :deletedAt WHERE id = :id";
            jdbcClient.sql(sql)
                    .param("deletedAt", LocalDateTime.now())
                    .param("id", id)
                    .update();
        } else {
            // Boolean 模式：设置 deleted = true/1
            sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                    " SET deleted = :deleted WHERE id = :id";
            jdbcClient.sql(sql)
                    .param("deleted", true)
                    .param("id", id)
                    .update();
        }

        // 失效缓存
        cacheHandler.evict(id);
    }

    /**
     * 删除实体
     *
     * @param entity 实体对象
     */
    public void delete(@NonNull T entity) {
        Object id = queryHelper.getIdValue(entity);
        if (id != null) {
            deleteById(id);
        }
    }

    /**
     * 批量删除实体（根据 ID）
     *
     * @param ids ID 集合
     */
    public void deleteAllById(@NonNull Iterable<?> ids) {
        for (Object id : ids) {
            deleteById(id);
        }
    }

    /**
     * 批量删除实体
     *
     * @param entities 实体集合
     */
    public void deleteAll(@NonNull Iterable<? extends T> entities) {
        for (T entity : entities) {
            delete(entity);
        }
    }
}