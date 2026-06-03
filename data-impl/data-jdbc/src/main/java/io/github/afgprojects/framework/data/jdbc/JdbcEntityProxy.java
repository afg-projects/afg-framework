package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
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
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationType;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
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
        this.updateHandler = new EntityUpdateHandler<>(entityClass, jdbcClient, metadata, queryHelper, dataManager, cacheHandler);
        this.softDeleteHandler = new EntitySoftDeleteHandler<>(entityClass, dialect, metadata, jdbcClient, cacheManager);
        this.queryExecutor = new EntityQueryExecutor<>(entityClass, jdbcClient, dialect, metadata, rowMapper, cacheHandler, softDeleteHandler, this);
        this.deleteHandler = new EntityDeleteHandler<>(entityClass, jdbcClient, dialect, metadata, softDeleteHandler, cacheHandler, queryHelper);
        this.conditionQueryHandler = new EntityConditionQueryHandler<>(entityClass, jdbcClient, dialect, metadata, rowMapper, dataManager, softDeleteHandler, this);
        this.conditionalHandler = new EntityConditionalHandler<>(entityClass, dialect, metadata, dataManager, cacheHandler);
        this.associationLoader = new AssociationLoader(dialect, dataManager);
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
        return insertHandler.insert(entity);
    }

    @Override
    public @NonNull List<T> insertAll(@NonNull Iterable<T> entities) {
        return insertHandler.insertAll(entities);
    }

    @Override
    public @NonNull T update(@NonNull T entity) {
        return updateHandler.update(entity);
    }

    @Override
    public @NonNull List<T> updateAll(@NonNull Iterable<T> entities) {
        return updateHandler.updateAll(entities);
    }

    @Override
    public @NonNull Optional<T> findById(@NonNull Object id) {
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
        deleteHandler.deleteById(id);
    }

    @Override
    public void delete(@NonNull T entity) {
        deleteHandler.delete(entity);
    }

    @Override
    public void deleteAllById(@NonNull Iterable<?> ids) {
        deleteHandler.deleteAllById(ids);
    }

    @Override
    public void deleteAll(@NonNull Iterable<? extends T> entities) {
        deleteHandler.deleteAll(entities);
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
    public @NonNull Page<T> findAll(@NonNull Condition condition, @NonNull PageRequest pageable) {
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
     * 获取租户ID
     */
    String getTenantId() {
        return tenantId;
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
            throw new IllegalArgumentException("batchSize must be greater than 0");
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
    }

    @Override
    public void restoreAllById(@NonNull Iterable<?> ids) {
        softDeleteHandler.restoreAllById(ids);
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Association '" + name + "' not found in entity " + entityClass.getSimpleName()
                ));

        Object idValue = getIdValue(entity);
        if (idValue == null) {
            throw new IllegalArgumentException("Entity must have an ID to fetch associations");
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
                .orElseThrow(() -> new IllegalArgumentException(
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
}
