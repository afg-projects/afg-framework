package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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

    /**
     * ID 列名（从元数据获取）
     */
    private final String idColumnName;

    /**
     * 软删除列名（从元数据获取）
     */
    private final String softDeleteColumnName;

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
        // 从元数据获取 ID 列名，向后兼容默认 "id"
        this.idColumnName = metadata.getIdField() != null ? metadata.getIdField().getColumnName() : "id";
        // 从元数据获取软删除列名，向后兼容默认值
        this.softDeleteColumnName = resolveSoftDeleteColumnName();
    }

    /**
     * 从元数据解析软删除列名
     */
    private String resolveSoftDeleteColumnName() {
        var softDeleteField = metadata.getSoftDeleteField();
        if (softDeleteField != null) {
            return softDeleteField.getColumnName();
        }
        // 向后兼容：根据策略返回默认列名
        if (softDeleteHandler.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP) {
            return "deleted_at";
        }
        return "deleted";
    }

    /**
     * 根据 ID 删除实体
     * <p>
     * 如果实体支持软删除，执行软删除；否则执行物理删除。
     *
     * @param id 实体 ID
     * @param entity 实体对象（用于生命周期回调，可为 null）
     */
    public void deleteById(@NonNull Object id, @Nullable T entity) {
        // 如果实体支持软删除，执行软删除
        if (softDeleteHandler.isSoftDeletable()) {
            softDeleteById(id);
            // 触发 afterDelete 生命周期回调（类似 JPA @PostRemove）
            if (entity != null) {
                LifecycleCallbacks.ifCallback(entity, cb -> cb.afterDelete(entity));
            }
            return;
        }

        // 物理删除
        String sql = "DELETE FROM " + dialect.quoteIdentifier(metadata.getTableName())
                + " WHERE " + dialect.quoteIdentifier(idColumnName) + " = :id";
        jdbcClient.sql(sql)
                .param("id", id)
                .update();

        // 触发 afterDelete 生命周期回调（类似 JPA @PostRemove）
        if (entity != null) {
            LifecycleCallbacks.ifCallback(entity, cb -> cb.afterDelete(entity));
        }

        // 失效缓存
        cacheHandler.evict(id);
    }

    /**
     * 根据 ID 删除实体（不触发生命周期回调）
     * <p>
     * 如果实体支持软删除，执行软删除；否则执行物理删除。
     *
     * @param id 实体 ID
     */
    public void deleteById(@NonNull Object id) {
        deleteById(id, null);
    }

    /**
     * 软删除指定 ID 的记录
     *
     * @param id 实体 ID
     */
    private void softDeleteById(@NonNull Object id) {
        String sql;
        if (softDeleteHandler.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP) {
            // 时间戳模式：设置软删除列（deleted_at）为当前时间
            sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                    " SET " + dialect.quoteIdentifier(softDeleteColumnName) + " = :deletedAt WHERE "
                    + dialect.quoteIdentifier(idColumnName) + " = :id";
            jdbcClient.sql(sql)
                    .param("deletedAt", LocalDateTime.now())
                    .param("id", id)
                    .update();
        } else {
            // Boolean 模式：设置软删除列（deleted）为 true/1
            sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                    " SET " + dialect.quoteIdentifier(softDeleteColumnName) + " = :deleted WHERE "
                    + dialect.quoteIdentifier(idColumnName) + " = :id";
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
            deleteById(id, entity);
        }
    }

    /**
     * 批量删除实体（根据 ID）
     * <p>
     * 使用 IN 子句批量操作，每批最多 500 条，减少数据库往返次数。
     * <p>
     * <b>注意：</b>此方法不触发 {@code afterDelete} 生命周期回调，因为仅有 ID 无法获取实体对象。
     * 如需回调，请使用 {@link JdbcEntityProxy#deleteAllById}，它会逐条触发。
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
                    " SET " + dialect.quoteIdentifier(softDeleteColumnName) + " = :deletedAt WHERE "
                    + dialect.quoteIdentifier(idColumnName) + " IN (:ids)";
            jdbcClient.sql(sql)
                    .param("deletedAt", LocalDateTime.now())
                    .param("ids", ids)
                    .update();
        } else {
            sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                    " SET " + dialect.quoteIdentifier(softDeleteColumnName) + " = :deleted WHERE "
                    + dialect.quoteIdentifier(idColumnName) + " IN (:ids)";
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
        String sql = "DELETE FROM " + dialect.quoteIdentifier(metadata.getTableName())
                + " WHERE " + dialect.quoteIdentifier(idColumnName) + " IN (:ids)";
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
