package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.entity.SoftDeletable;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.entity.TimestampSoftDeletable;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCache;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体软删除处理器
 * <p>
 * 负责处理软删除相关的逻辑，包括软删除、恢复、物理删除等操作
 */
class EntitySoftDeleteHandler<T> {

    private final Class<T> entityClass;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;
    private final JdbcClient jdbcClient;
    private final @Nullable SoftDeleteStrategy softDeleteStrategy;
    private final EntityCacheManager cacheManager;

    EntitySoftDeleteHandler(Class<T> entityClass, Dialect dialect, EntityMetadata<T> metadata,
                            JdbcClient jdbcClient, @Nullable EntityCacheManager cacheManager) {
        this.entityClass = entityClass;
        this.dialect = dialect;
        this.metadata = metadata;
        this.jdbcClient = jdbcClient;
        this.cacheManager = cacheManager;
        this.softDeleteStrategy = detectSoftDeleteStrategy(entityClass);
    }

    /**
     * 检测实体的软删除策略
     */
    private static @Nullable SoftDeleteStrategy detectSoftDeleteStrategy(Class<?> entityClass) {
        if (TimestampSoftDeletable.class.isAssignableFrom(entityClass)) {
            return SoftDeleteStrategy.TIMESTAMP;
        }
        if (SoftDeletable.class.isAssignableFrom(entityClass)) {
            return SoftDeleteStrategy.BOOLEAN;
        }
        return null;
    }

    /**
     * 获取当前软删除策略
     */
    @Nullable SoftDeleteStrategy getSoftDeleteStrategy() {
        return softDeleteStrategy;
    }

    /**
     * 判断实体是否支持软删除
     */
    boolean isSoftDeletable() {
        return softDeleteStrategy != null;
    }

    /**
     * 恢复指定 ID 的记录
     */
    void restoreById(Object id) {
        if (softDeleteStrategy == null) {
            throw new UnsupportedOperationException(
                "Entity " + entityClass.getSimpleName() + " does not support soft delete"
            );
        }

        if (softDeleteStrategy == SoftDeleteStrategy.TIMESTAMP) {
            String sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                   " SET deleted_at = NULL WHERE id = :id";
            jdbcClient.sql(sql)
                .param("id", id)
                .update();
        } else {
            String sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                   " SET deleted = :deleted WHERE id = :id";
            jdbcClient.sql(sql)
                .param("id", id)
                .param("deleted", false)
                .update();
        }

        evictCacheById(id);
    }

    /**
     * 恢复所有指定 ID 的记录（使用 IN 子句批量操作）
     */
    void restoreAllById(Iterable<?> ids) {
        List<Object> idList = new ArrayList<>();
        ids.forEach(idList::add);
        if (idList.isEmpty()) {
            return;
        }

        if (softDeleteStrategy == SoftDeleteStrategy.TIMESTAMP) {
            String sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                   " SET deleted_at = NULL WHERE id IN (:ids)";
            jdbcClient.sql(sql)
                .param("ids", idList)
                .update();
        } else {
            String sql = "UPDATE " + dialect.quoteIdentifier(metadata.getTableName()) +
                   " SET deleted = :deleted WHERE id IN (:ids)";
            jdbcClient.sql(sql)
                .param("ids", idList)
                .param("deleted", false)
                .update();
        }

        idList.forEach(this::evictCacheById);
    }

    /**
     * 物理删除指定 ID 的记录（忽略软删除）
     */
    void hardDeleteById(Object id) {
        String sql = "DELETE FROM " + dialect.quoteIdentifier(metadata.getTableName()) + " WHERE id = :id";
        jdbcClient.sql(sql)
            .param("id", id)
            .update();

        evictCacheById(id);
    }

    /**
     * 物理删除所有指定 ID 的记录（忽略软删除，使用 IN 子句批量操作）
     */
    void hardDeleteAllById(Iterable<?> ids) {
        List<Object> idList = new ArrayList<>();
        ids.forEach(idList::add);
        if (idList.isEmpty()) {
            return;
        }

        String sql = "DELETE FROM " + dialect.quoteIdentifier(metadata.getTableName()) + " WHERE id IN (:ids)";
        jdbcClient.sql(sql)
            .param("ids", idList)
            .update();

        idList.forEach(this::evictCacheById);
    }

    /**
     * 追加软删除过滤条件到 SQL
     *
     * @param sqlBuilder     SQL 构建器
     * @param hasWhereClause 是否已有 WHERE 子句
     * @param includeDeleted 是否包含已删除记录
     */
    void appendSoftDeleteFilter(StringBuilder sqlBuilder, boolean hasWhereClause, boolean includeDeleted) {
        if (softDeleteStrategy == null || includeDeleted) {
            return;
        }

        if (hasWhereClause) {
            sqlBuilder.append(" AND ");
        } else {
            sqlBuilder.append(" WHERE ");
        }

        if (softDeleteStrategy == SoftDeleteStrategy.TIMESTAMP) {
            sqlBuilder.append("deleted_at IS NULL");
        } else {
            sqlBuilder.append("deleted = false");
        }
    }

    /**
     * 构建软删除过滤 SQL 片段
     * <p>
     * 返回需要追加到 SQL 语句末尾的过滤条件（如 " AND deleted = false" 或 " WHERE deleted_at IS NULL"）。
     * 如果实体不支持软删除或已包含已删除记录，返回空字符串。
     *
     * @param hasWhereClause 是否已有 WHERE 子句
     * @param includeDeleted 是否包含已删除记录
     * @return SQL 过滤片段
     */
    String buildSoftDeleteFilterSql(boolean hasWhereClause, boolean includeDeleted) {
        if (softDeleteStrategy == null || includeDeleted) {
            return "";
        }

        String filter = (softDeleteStrategy == SoftDeleteStrategy.TIMESTAMP)
                ? "deleted_at IS NULL"
                : "deleted = false";
        return (hasWhereClause ? " AND " : " WHERE ") + filter;
    }

    /**
     * 获取纯软删除过滤条件（不含 WHERE/AND 前缀）
     * <p>
     * 用于手动拼接 WHERE 子句时，调用方自行处理前缀。
     *
     * @return 过滤条件（如 "deleted_at IS NULL" 或 "deleted = false"），如果实体不支持软删除返回 null
     */
    @Nullable String getSoftDeleteFilterCondition() {
        if (softDeleteStrategy == null) {
            return null;
        }
        return (softDeleteStrategy == SoftDeleteStrategy.TIMESTAMP)
                ? "deleted_at IS NULL"
                : "deleted = false";
    }

    /**
     * 构建软删除 SET 子句
     * <p>
     * 使用位置参数占位符，调用方需在参数列表中提供对应的值。
     * TIMESTAMP 模式使用 {@code ?} 占位符（调用方传入 {@link java.time.LocalDateTime#now()}），
     * BOOLEAN 模式使用 {@code ?} 占位符（调用方传入 {@code true}）。
     *
     * @return SET 子句（如 "deleted_at = ?" 或 "deleted = ?"）
     */
    String buildSoftDeleteSetClause() {
        if (softDeleteStrategy == null) {
            throw new UnsupportedOperationException(
                "Entity " + entityClass.getSimpleName() + " does not support soft delete"
            );
        }

        if (softDeleteStrategy == SoftDeleteStrategy.TIMESTAMP) {
            return "deleted_at = ?";
        } else {
            return "deleted = ?";
        }
    }

    /**
     * 失效缓存
     */
    private void evictCacheById(Object id) {
        if (cacheManager != null) {
            String cacheKey = buildCacheKey(id);
            EntityCache<T> cache = cacheManager.getCacheIfPresent(metadata.getEntityClass());
            if (cache != null) {
                cache.evict(cacheKey);
            }
        }
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(Object id) {
        return metadata.getTableName() + ":" + id;
    }
}
