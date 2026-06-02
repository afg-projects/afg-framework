package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.entity.Versioned;
import io.github.afgprojects.framework.data.core.exception.OptimisticLockException;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体更新操作处理器
 * <p>
 * 封装实体更新相关的逻辑，包括单条更新、批量更新、乐观锁检查等操作。
 * 从 JdbcEntityProxy 中提取，降低类的复杂度。
 *
 * @param <T> 实体类型
 */
public class EntityUpdateHandler<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final EntityMetadata<T> metadata;
    private final EntityQueryHelper<T> queryHelper;
    private final JdbcDataManager dataManager;
    private final EntityCacheHandler<T> cacheHandler;

    public EntityUpdateHandler(Class<T> entityClass, JdbcClient jdbcClient,
                               EntityMetadata<T> metadata, EntityQueryHelper<T> queryHelper,
                               JdbcDataManager dataManager, EntityCacheHandler<T> cacheHandler) {
        this.entityClass = entityClass;
        this.jdbcClient = jdbcClient;
        this.metadata = metadata;
        this.queryHelper = queryHelper;
        this.dataManager = dataManager;
        this.cacheHandler = cacheHandler;
    }

    /**
     * 更新单个实体
     *
     * @param entity 实体对象
     * @return 更新后的实体
     * @throws OptimisticLockException 如果乐观锁检查失败
     */
    public @NonNull T update(@NonNull T entity) {
        boolean isVersioned = Versioned.class.isAssignableFrom(entityClass);
        String sql = queryHelper.buildUpdateSql(isVersioned);
        List<Object> params = queryHelper.extractUpdateParams(entity, isVersioned);
        int affectedRows = dataManager.executeUpdate(sql, params);

        // 乐观锁检测：如果实体实现了 Versioned 接口，检查更新行数
        if (isVersioned && affectedRows == 0) {
            Object id = queryHelper.getIdValue(entity);
            long version = ((Versioned) entity).getVersion();
            throw new OptimisticLockException(entityClass.getSimpleName(), id, version);
        }

        // 更新成功后，递增实体中的版本号
        if (isVersioned) {
            ((Versioned) entity).incrementVersion();
        }

        // 失效缓存
        evictCache(entity);

        return entity;
    }

    /**
     * 批量更新实体
     *
     * @param entities 实体集合
     * @return 更新后的实体列表
     */
    public @NonNull List<T> updateAll(@NonNull Iterable<T> entities) {
        List<T> result = new ArrayList<>();
        for (T entity : entities) {
            result.add(update(entity));
        }
        return result;
    }

    /**
     * 失效实体缓存
     */
    private void evictCache(@NonNull T entity) {
        Object id = queryHelper.getIdValue(entity);
        if (id != null) {
            cacheHandler.evict(id);
        }
    }
}
