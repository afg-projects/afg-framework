package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.EntityQuery;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.entity.AuditableContext;
import io.github.afgprojects.framework.data.core.entity.Treeable;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.event.EntityChangedEvent;
import io.github.afgprojects.framework.data.core.event.EntityChangedEventPublisher;
import io.github.afgprojects.framework.data.core.mapper.Projection;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.ProjectedQuery;
import io.github.afgprojects.framework.data.core.query.TreeQuery;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationType;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.jdbc.query.JdbcTreeQuery;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.*;

/**
 * JDBC EntityProxy 实现（基于 Spring JdbcClient）
 * <p>
 * 此类已重构，将职责委托给辅助类，按功能分组如下：
 * <p>
 * <strong>查询操作：</strong>
 * <ul>
 *   <li>{@link EntityQueryHelper} - SQL 构建、参数提取、结果映射</li>
 *   <li>{@link EntityQueryExecutor} - 基础查询执行</li>
 *   <li>{@link EntityConditionQueryHandler} - 条件查询处理</li>
 * </ul>
 * <p>
 * <strong>数据变更操作：</strong>
 * <ul>
 *   <li>{@link EntityInsertHandler} - 插入操作处理</li>
 *   <li>{@link EntityUpdateHandler} - 更新操作处理</li>
 *   <li>{@link EntityDeleteHandler} - 删除操作处理</li>
 *   <li>{@link EntityConditionalHandler} - 条件更新/删除处理</li>
 * </ul>
 * <p>
 * <strong>特殊功能：</strong>
 * <ul>
 *   <li>{@link EntitySoftDeleteHandler} - 软删除处理</li>
 *   <li>{@link AssociationLoader} - 关联关系加载</li>
 *   <li>{@link EntityCacheHandler} - 缓存处理</li>
 * </ul>
 * <p>
 * 实现 {@link ProxyStateProvider} 接口，让 handler 可以直接读取状态，
 * 避免在每个方法中手动同步。
 */
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity"})
@Slf4j
public class JdbcEntityProxy<T> implements EntityProxy<T>, ProxyStateProvider {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    final JdbcDataManager dataManager;
    private final EntityMetadata<T> metadata;
    final RowMapper<T> rowMapper;
    private final TypeHandlerRegistry typeHandlerRegistry;

    /**
     * 查询辅助类
     */
    private final EntityQueryHelper<T> queryHelper;

    /**
     * 实体缓存处理器
     */
    private final EntityCacheHandler<T> cacheHandler;

    /**
     * 插入操作处理器
     */
    private final EntityInsertHandler<T> insertHandler;

    /**
     * 更新操作处理器
     */
    private final EntityUpdateHandler<T> updateHandler;

    /**
     * 查询执行器
     */
    private final EntityQueryExecutor<T> queryExecutor;

    /**
     * 删除操作处理器
     */
    private final EntityDeleteHandler<T> deleteHandler;

    /**
     * 条件查询处理器
     */
    private final EntityConditionQueryHandler<T> conditionQueryHandler;

    /**
     * 条件操作处理器
     */
    private final EntityConditionalHandler<T> conditionalHandler;

    /**
     * 软删除处理器
     */
    private final EntitySoftDeleteHandler<T> softDeleteHandler;

    /**
     * 关联加载器
     */
    private final AssociationLoader associationLoader;

    private List<DataScope> dataScopes = new ArrayList<>();
    private String tenantId;
    private boolean includeDeleted = false;

    /**
     * 要急加载的关联字段名集合
     */
    private final Set<String> eagerFetchAssociations = new LinkedHashSet<>();

    public JdbcEntityProxy(Class<T> entityClass, JdbcClient jdbcClient, Dialect dialect, JdbcDataManager dataManager) {
        this(entityClass, jdbcClient, dialect, dataManager, null);
    }

    public JdbcEntityProxy(Class<T> entityClass, JdbcClient jdbcClient, Dialect dialect,
                           JdbcDataManager dataManager, @Nullable EntityCacheManager cacheManager) {
        this(entityClass, jdbcClient, dialect, dataManager, cacheManager, TypeHandlerRegistry.defaultRegistry());
    }

    public JdbcEntityProxy(Class<T> entityClass, JdbcClient jdbcClient, Dialect dialect,
                           JdbcDataManager dataManager, @Nullable EntityCacheManager cacheManager,
                           TypeHandlerRegistry typeHandlerRegistry) {
        this.entityClass = entityClass;
        this.jdbcClient = jdbcClient;
        this.dialect = dialect;
        this.dataManager = dataManager;
        this.cacheHandler = new EntityCacheHandler<>(entityClass, cacheManager, dataManager.getTenantContextHolder());
        this.metadata = dataManager.getEntityMetadata(entityClass);
        this.queryHelper = new EntityQueryHelper<>(entityClass, dialect, metadata, typeHandlerRegistry);
        this.rowMapper = queryHelper::mapRow;
        this.typeHandlerRegistry = typeHandlerRegistry;
        this.insertHandler = new EntityInsertHandler<>(entityClass, jdbcClient, dialect, metadata, queryHelper, dataManager, cacheHandler);
        this.insertHandler.setAuditableContext(dataManager.getAuditableContext());
        this.insertHandler.setFieldEncryptor(dataManager.getFieldEncryptor());
        this.insertHandler.setBlindIndexProvider(dataManager.getBlindIndexProvider());
        this.insertHandler.setIdGenerator(dataManager.getIdGenerator());
        this.updateHandler = new EntityUpdateHandler<>(entityClass, jdbcClient, metadata, queryHelper, dataManager, cacheHandler);
        this.updateHandler.setAuditableContext(dataManager.getAuditableContext());
        this.updateHandler.setFieldEncryptor(dataManager.getFieldEncryptor());
        this.updateHandler.setBlindIndexProvider(dataManager.getBlindIndexProvider());
        this.softDeleteHandler = new EntitySoftDeleteHandler<>(entityClass, dialect, metadata, jdbcClient, cacheManager);
        this.queryExecutor = new EntityQueryExecutor<>(entityClass, jdbcClient, dialect, metadata, rowMapper, cacheHandler, softDeleteHandler, this);
        this.deleteHandler = new EntityDeleteHandler<>(entityClass, jdbcClient, dialect, metadata, softDeleteHandler, cacheHandler, queryHelper);
        this.conditionQueryHandler = new EntityConditionQueryHandler<>(entityClass, jdbcClient, dialect, metadata, rowMapper, dataManager, softDeleteHandler, this);
        this.conditionalHandler = new EntityConditionalHandler<>(entityClass, dialect, metadata, dataManager, cacheHandler);
        this.associationLoader = new AssociationLoader(dialect, dataManager);

        // 将 FieldEncryptor 注入到 EntityMapper（用于 SELECT 后自动解密）
        this.queryHelper.getEntityMapper().setFieldEncryptor(dataManager.getFieldEncryptor());
    }

    // ==================== 基础 CRUD ====================

    @Override
    public @NonNull T save(@NonNull T entity) {
        Object id = queryHelper.getIdValue(entity);
        if (id == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    @Override
    public @NonNull List<T> saveAll(@NonNull Iterable<? extends T> entities) {
        // 将 Iterable 转换为 List，同时记录原始索引
        List<T> entityList = new ArrayList<>();
        entities.forEach(entityList::add);

        if (entityList.isEmpty()) {
            return entityList;
        }

        // 分离新实体（需要插入）和已有实体（需要更新），记录原始索引
        Map<Integer, T> insertMap = new LinkedHashMap<>();
        Map<Integer, T> updateMap = new LinkedHashMap<>();

        for (int i = 0; i < entityList.size(); i++) {
            T entity = entityList.get(i);
            Object id = queryHelper.getIdValue(entity);
            if (id == null) {
                insertMap.put(i, entity);
            } else {
                updateMap.put(i, entity);
            }
        }

        // 批量插入新实体
        List<T> insertedEntities = new ArrayList<>();
        if (!insertMap.isEmpty()) {
            insertedEntities = insertAll(insertMap.values());
        }

        // 逐条更新已有实体（更新操作通常需要检查乐观锁，不适合批量）
        Map<Integer, T> updateResults = new LinkedHashMap<>();
        for (Map.Entry<Integer, T> entry : updateMap.entrySet()) {
            updateResults.put(entry.getKey(), update(entry.getValue()));
        }

        // 按原始顺序组装结果
        List<T> result = new ArrayList<>(entityList.size());
        int insertedIndex = 0;
        for (int i = 0; i < entityList.size(); i++) {
            if (insertMap.containsKey(i)) {
                result.add(insertedEntities.get(insertedIndex++));
            } else {
                result.add(updateResults.get(i));
            }
        }

        return result;
    }

    @Override
    public @NonNull T insert(@NonNull T entity) {
        T result = insertHandler.insert(entity);
        publishChangedEvent(result, null, EntityChangedEvent.ChangeType.CREATED);
        return result;
    }

    @Override
    public @NonNull List<T> insertAll(@NonNull Iterable<T> entities) {
        List<T> result = insertHandler.insertAll(entities);
        for (T entity : result) {
            publishChangedEvent(entity, null, EntityChangedEvent.ChangeType.CREATED);
        }
        return result;
    }

    @Override
    public @NonNull T update(@NonNull T entity) {
        // 查询更新前的实体状态（用于事件）
        T oldEntity = null;
        Object id = queryHelper.getIdValue(entity);
        if (id != null) {
            oldEntity = queryExecutor.findById(id).orElse(null);
        }

        T result = updateHandler.update(entity);
        publishChangedEvent(result, oldEntity, EntityChangedEvent.ChangeType.UPDATED);
        return result;
    }

    @Override
    public @NonNull List<T> updateAll(@NonNull Iterable<T> entities) {
        List<T> result = updateHandler.updateAll(entities);
        for (T entity : result) {
            publishChangedEvent(entity, null, EntityChangedEvent.ChangeType.UPDATED);
        }
        return result;
    }

    @Override
    public @NonNull Optional<T> findById(@Nullable Object id) {
        return queryExecutor.findById(id);
    }

    @Override
    public @NonNull List<T> findAll() {
        return queryExecutor.findAll();
    }

    @Override
    public @NonNull List<T> findAllById(@NonNull Iterable<?> ids) {
        return queryExecutor.findAllById(ids);
    }

    @Override
    public long count() {
        return queryExecutor.count();
    }

    @Override
    public boolean existsById(@NonNull Object id) {
        return queryExecutor.existsById(id);
    }

    @Override
    public void deleteById(@NonNull Object id) {
        // 查询删除前的实体状态（用于事件和生命周期回调）
        T entity = queryExecutor.findById(id).orElse(null);
        deleteHandler.deleteById(id, entity);
        if (entity != null) {
            publishChangedEvent(entity, null, EntityChangedEvent.ChangeType.DELETED);
        }
    }

    @Override
    public void delete(@NonNull T entity) {
        deleteHandler.delete(entity);
        publishChangedEvent(entity, null, EntityChangedEvent.ChangeType.DELETED);
    }

    @Override
    public void deleteAllById(@NonNull Iterable<?> ids) {
        for (Object id : ids) {
            deleteById(id);
        }
    }

    @Override
    public void deleteAll(@NonNull Iterable<? extends T> entities) {
        for (T entity : entities) {
            delete(entity);
        }
    }

    // ==================== 条件查询 ====================

    /**
     * 获取条件查询构建器
     * <p>
     * 返回新的查询构建器实例，支持链式调用配置查询条件。
     *
     * @return 查询构建器
     */
    @Override
    public @NonNull EntityQuery<T> query() {
        return new JdbcEntityQuery<>(this);
    }

    @Override
    public @NonNull List<T> findAll(@NonNull Condition condition) {
        return conditionQueryHandler.findAll(condition);
    }

    @Override
    public @NonNull PageData<T> findAll(@NonNull Condition condition, @NonNull PageRequest pageable) {
        return conditionQueryHandler.findAll(condition, pageable);
    }

    // ==================== 条件更新/删除 ====================

    @Override
    public long updateAll(@NonNull Condition condition, @NonNull Map<String, Object> updates) {
        return conditionalHandler.updateAll(condition, updates);
    }

    @Override
    public long deleteByCondition(@NonNull Condition condition) {
        return conditionalHandler.deleteByCondition(condition);
    }

    // ==================== 企业级特性（内部状态，供 JdbcEntityQuery 使用） ====================

    /**
     * 获取数据权限列表
     */
    List<DataScope> getDataScopes() {
        return dataScopes;
    }

    /**
     * 获取显式设置的租户ID
     */
    @Override
    public @Nullable String getTenantId() {
        return tenantId;
    }

    /**
     * 解析有效的租户ID
     * <p>
     * 优先使用通过 withTenant() 显式设置的租户ID，
     * 其次回退到 TenantContextHolder 中的上下文租户ID。
     */
    @Override
    public @Nullable String resolveEffectiveTenantId() {
        if (tenantId != null) {
            return tenantId;
        }
        return dataManager.getTenantContextHolder().getTenantId();
    }

    /**
     * 是否包含已删除记录
     */
    @Override
    public boolean isIncludeDeleted() {
        return includeDeleted;
    }

    /**
     * 设置批量插入的批次大小
     *
     * @param batchSize 批次大小，必须大于 0
     * @return this
     */
    public @NonNull EntityProxy<T> withBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "batchSize must be greater than 0");
        }
        this.insertHandler.setBatchSize(batchSize);
        return this;
    }

    /**
     * 获取当前批次大小
     *
     * @return 批次大小
     */
    public int getBatchSize() {
        return insertHandler.getBatchSize();
    }

    /**
     * 获取实体类
     *
     * @return 实体类
     */
    @NonNull
    public Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * 获取底层 DataManager
     *
     * @return JdbcDataManager 实例
     */
    @NonNull
    public JdbcDataManager getDataManager() {
        return dataManager;
    }

    /**
     * 获取 JdbcClient
     *
     * @return JdbcClient
     */
    @NonNull
    public JdbcClient getJdbcClient() {
        return jdbcClient;
    }

    /**
     * 获取数据库方言
     *
     * @return 方言
     */
    @NonNull
    public Dialect getDialect() {
        return dialect;
    }

    /**
     * 获取行映射器
     *
     * @return 行映射器
     */
    @NonNull
    public RowMapper<T> getRowMapper() {
        return rowMapper;
    }

    /**
     * 获取类型处理器注册表
     */
    @NonNull
    public TypeHandlerRegistry getTypeHandlerRegistry() {
        return typeHandlerRegistry;
    }

    // ==================== 软删除扩展 ====================

    /**
     * 获取当前软删除策略
     *
     * @return 软删除策略，null 表示不支持软删除
     */
    public @Nullable SoftDeleteStrategy getSoftDeleteStrategy() {
        return softDeleteHandler.getSoftDeleteStrategy();
    }

    /**
     * 获取软删除处理器
     */
    EntitySoftDeleteHandler<T> getSoftDeleteHandler() {
        return softDeleteHandler;
    }

    /**
     * 判断实体是否支持软删除
     *
     * @return 支持软删除返回 true
     */
    public boolean isSoftDeletable() {
        return softDeleteHandler.isSoftDeletable();
    }

    @Override
    public void restoreById(@NonNull Object id) {
        softDeleteHandler.restoreById(id);
        // 查询恢复后的实体状态（用于事件）
        T entity = queryExecutor.findById(id).orElse(null);
        if (entity != null) {
            publishChangedEvent(entity, null, EntityChangedEvent.ChangeType.RESTORED);
        }
    }

    @Override
    public void restoreAllById(@NonNull Iterable<?> ids) {
        for (Object id : ids) {
            restoreById(id);
        }
    }

    /**
     * 物理删除指定 ID 的记录（忽略软删除）
     *
     * @param id 实体 ID
     */
    public void hardDeleteById(@NonNull Object id) {
        softDeleteHandler.hardDeleteById(id);
    }

    /**
     * 物理删除实体（忽略软删除）
     *
     * @param entity 实体对象
     */
    public void hardDelete(@NonNull T entity) {
        Object id = queryHelper.getIdValue(entity);
        if (id != null) {
            softDeleteHandler.hardDeleteById(id);
        }
    }

    /**
     * 物理删除所有指定 ID 的记录（忽略软删除）
     *
     * @param ids ID 列表
     */
    public void hardDeleteAllById(@NonNull Iterable<?> ids) {
        softDeleteHandler.hardDeleteAllById(ids);
    }

    // ==================== 关联查询 ====================

    /**
     * 获取要急加载的关联字段集合
     */
    Set<String> getEagerFetchAssociations() {
        return Collections.unmodifiableSet(eagerFetchAssociations);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> @NonNull R fetch(@NonNull T entity, @NonNull String name) {
        RelationMetadata relation = metadata.getRelation(name)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                        "Association '" + name + "' not found in entity " + entityClass.getSimpleName()
                ));

        Object idValue = getIdValue(entity);
        if (idValue == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "Entity must have an ID to fetch associations");
        }

        return (R) doFetchAssociation(entity, idValue, relation);
    }

    /**
     * 执行关联数据加载
     *
     * @param entity   实体对象
     * @param idValue  实体ID
     * @param relation 关联元数据
     * @return 关联数据
     */
    private Object doFetchAssociation(T entity, Object idValue, RelationMetadata relation) {
        return associationLoader.fetchAssociation(entity, idValue, relation, entityClass, metadata);
    }

    @Override
    public void fetchAll(@NonNull Iterable<T> entities, @NonNull String name) {
        RelationMetadata relation = metadata.getRelation(name)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                        "Association '" + name + "' not found in entity " + entityClass.getSimpleName()
                ));

        // 批量加载关联数据，避免 N+1 问题
        doFetchAllAssociations(entities, relation);
    }

    /**
     * 批量加载关联数据
     * <p>
     * 使用批量查询优化，避免 N+1 查询问题。
     */
    private void doFetchAllAssociations(Iterable<T> entities, RelationMetadata relation) {
        // 收集所有实体 ID
        List<Object> ids = new ArrayList<>();
        for (T entity : entities) {
            Object id = queryHelper.getIdValue(entity);
            if (id != null) {
                ids.add(id);
            }
        }

        if (ids.isEmpty()) {
            return;
        }

        RelationType relationType = relation.getRelationType();
        Class<?> targetEntityClass = relation.getTargetEntityClass();

        // 根据关联类型执行批量加载
        switch (relationType) {
            case MANY_TO_ONE:
                associationLoader.fetchAllManyToOne(entities, relation, targetEntityClass, metadata);
                break;
            case ONE_TO_ONE:
                associationLoader.fetchAllOneToOne(entities, ids, relation, targetEntityClass, metadata);
                break;
            case ONE_TO_MANY: {
                Map<Object, List<Object>> foreignKeyToTargets =
                        associationLoader.fetchAllOneToMany(ids, relation, targetEntityClass);
                // 将关联数据设置回源实体
                String fieldName = relation.getFieldName();
                for (T entity : entities) {
                    Object id = queryHelper.getIdValue(entity);
                    if (id != null) {
                        List<Object> targets = foreignKeyToTargets.get(id);
                        associationLoader.setFieldValue(entity, fieldName, targets != null ? targets : new ArrayList<>());
                    }
                }
                break;
            }
            case MANY_TO_MANY: {
                Map<Object, Set<Object>> sourceIdToTargets =
                        associationLoader.fetchAllManyToMany(ids, relation, targetEntityClass);
                // 将关联数据设置回源实体
                String fieldName = relation.getFieldName();
                for (T entity : entities) {
                    Object id = queryHelper.getIdValue(entity);
                    if (id != null) {
                        Set<Object> targets = sourceIdToTargets.get(id);
                        associationLoader.setFieldValue(entity, fieldName, targets != null ? targets : new LinkedHashSet<>());
                    }
                }
                break;
            }
        }
    }

    /**
     * 获取 ID 值
     */
    private @Nullable Object getIdValue(T entity) {
        return queryHelper.getIdValue(entity);
    }

    /**
     * 获取缓存管理器
     *
     * @return 缓存管理器，可能为 null
     */
    public @Nullable EntityCacheManager getCacheManager() {
        return cacheHandler.getCacheManager();
    }

    /**
     * 获取缓存处理器
     *
     * @return 缓存处理器
     */
    public EntityCacheHandler<T> getCacheHandler() {
        return cacheHandler;
    }

    // ==================== 树形查询 ====================

    /**
     * 获取树形结构查询接口
     * <p>
     * 仅当实体类型实现 {@link Treeable} 接口时可用。
     * 对于非树形实体，抛出 {@link io.github.afgprojects.framework.commons.exception.BusinessException}。
     *
     * @param <S> 实体类型（自动推断为 T &amp; Treeable）
     * @return 树形查询接口
     */
    @Override
    @SuppressWarnings("unchecked")
    public @NonNull TreeQuery<Treeable<?>> treeQuery() {
        if (!metadata.hasTrait(EntityTrait.TREEABLE)) {
            throw new io.github.afgprojects.framework.commons.exception.BusinessException(
                    io.github.afgprojects.framework.commons.exception.CommonErrorCode.PARAM_ERROR,
                    "Entity " + entityClass.getSimpleName() + " does not implement Treeable interface");
        }
        JdbcTreeQuery<T> treeQuery = new JdbcTreeQuery<>(this);
        // The double cast is necessary due to the self-referential generic on Treeable<T>
        // which is incompatible with the wildcard capture on TreeQuery<Treeable<?>>.
        @SuppressWarnings("PMD.UnnecessaryCast")
        TreeQuery<Treeable<?>> result = (TreeQuery<Treeable<?>>) (TreeQuery<?>) treeQuery;
        return result;
    }

    // ==================== 事件发布 ====================

    /**
     * 发布实体变更事件
     * <p>
     * 在保存、更新、删除、恢复操作后调用，
     * 通过 {@link EntityChangedEventPublisher} 发布事件。
     * 事件发布失败不影响业务操作。
     *
     * @param entity     变更后的实体
     * @param oldEntity  变更前的实体（仅 UPDATE 时有值）
     * @param changeType 变更类型
     */
    private void publishChangedEvent(@NonNull T entity, @Nullable T oldEntity,
                                      EntityChangedEvent.ChangeType changeType) {
        try {
            EntityChangedEventPublisher publisher = dataManager.getEntityChangedEventPublisher();
            EntityChangedEvent<T> event = EntityChangedEvent.<T>builder()
                    .entityType(entityClass)
                    .entity(entity)
                    .oldEntity(oldEntity)
                    .changeType(changeType)
                    .timestamp(java.time.Instant.now())
                    .build();
            publisher.publish(event);
        } catch (Exception e) {
            // 事件发布失败不应影响业务操作
            log.warn("Failed to publish EntityChangedEvent for {}: {}",
                    entityClass.getSimpleName(), e.getMessage());
        }
    }
}
