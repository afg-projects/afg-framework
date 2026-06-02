package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
     * <p>
     * 使用 IN 子句批量操作，每批最多 500 条，减少数据库往返次数。
     *
     * @param ids ID 集合
     */
    public void deleteAllById(@NonNull Iterable<?> ids) {
        List<Object> idList = new ArrayList<>();
        ids.forEach(idList::add);
        if (idList.isEmpty()) {
            return;
        }

        // 分批处理
        int batchSize = 500;
        for (int i = 0; i < idList.size(); i += batchSize) {
            List<Object> batch = idList.subList(i, Math.min(i + batchSize, idList.size()));
            if (softDeleteHandler.isSoftDeletable()) {
                batchSoftDeleteByIds(batch);
            } else {
                batchPhysicalDeleteByIds(batch);
            }
            batch.forEach(cacheHandler::evict);
        }
    }

    /**
     * 批量软删除
     */
    private void batchSoftDeleteByIds(List<Object> ids) {
        String sql;
        if (softDeleteHandler.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP) {
            sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                    " SET deleted_at = :deletedAt WHERE id IN (:ids)";
            jdbcClient.sql(sql)
                    .param("deletedAt", LocalDateTime.now())
                    .param("ids", ids)
                    .update();
        } else {
            sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                    " SET deleted = :deleted WHERE id IN (:ids)";
            jdbcClient.sql(sql)
                    .param("deleted", true)
                    .param("ids", ids)
                    .update();
        }
    }

    /**
     * 批量物理删除
     */
    private void batchPhysicalDeleteByIds(List<Object> ids) {
        String sql = "DELETE FROM " + dialect.quoteIdentifier(metadata.getTableName()) + " WHERE id IN (:ids)";
        jdbcClient.sql(sql)
                .param("ids", ids)
                .update();
    }

    /**
     * 批量删除实体
     * <p>
     * 提取所有实体 ID 后委托给 {@link #deleteAllById} 批量处理，
     * 避免逐条删除导致的多次数据库往返。
     *
     * @param entities 实体集合
     */
    public void deleteAll(@NonNull Iterable<? extends T> entities) {
        List<Object> ids = new ArrayList<>();
        for (T entity : entities) {
            Object id = queryHelper.getIdValue(entity);
            if (id != null) {
                ids.add(id);
            }
        }
        if (!ids.isEmpty()) {
            deleteAllById(ids);
        }
    }
}