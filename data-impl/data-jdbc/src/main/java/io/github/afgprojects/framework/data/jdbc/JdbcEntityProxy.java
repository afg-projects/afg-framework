package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.entity.Versioned;
import io.github.afgprojects.framework.data.core.exception.OptimisticLockException;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationType;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCache;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleEntityMetadata;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleFieldMetadata;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC EntityProxy 实现（基于 Spring JdbcClient）
 * <p>
 * 此类已重构，将职责委托给辅助类：
 * <ul>
 *   <li>{@link EntityQueryHelper} - SQL 构建、参数提取、结果映射</li>
 *   <li>{@link EntitySoftDeleteHandler} - 软删除处理</li>
 *   <li>{@link AssociationLoader} - 关联关系加载</li>
 * </ul>
 */
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity"})
public class JdbcEntityProxy<T> implements EntityProxy<T> {

    /**
     * 默认批次大小
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    final JdbcDataManager dataManager;
    private final SimpleEntityMetadata<T> metadata;
    final RowMapper<T> rowMapper;

    /**
     * 查询辅助类
     */
    private final EntityQueryHelper<T> queryHelper;

    /**
     * 实体缓存管理器（可选）
     */
    private final @Nullable EntityCacheManager cacheManager;

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
    private String dataSourceName;
    private boolean readOnly = false;
    private boolean includeDeleted = false;
    private int batchSize = DEFAULT_BATCH_SIZE;

    /**
     * 要急加载的关联字段名集合
     */
    private final Set<String> eagerFetchAssociations = new LinkedHashSet<>();

    public JdbcEntityProxy(Class<T> entityClass, JdbcClient jdbcClient, Dialect dialect, JdbcDataManager dataManager) {
        this(entityClass, jdbcClient, dialect, dataManager, null);
    }

    public JdbcEntityProxy(Class<T> entityClass, JdbcClient jdbcClient, Dialect dialect,
                           JdbcDataManager dataManager, @Nullable EntityCacheManager cacheManager) {
        this.entityClass = entityClass;
        this.jdbcClient = jdbcClient;
        this.dialect = dialect;
        this.dataManager = dataManager;
        this.cacheManager = cacheManager;
        this.metadata = new SimpleEntityMetadata<>(entityClass);
        this.queryHelper = new EntityQueryHelper<>(entityClass, dialect, metadata);
        this.rowMapper = queryHelper::mapRow;
        this.softDeleteHandler = new EntitySoftDeleteHandler<>(entityClass, dialect, metadata, jdbcClient, cacheManager);
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
    public @NonNull List<T> saveAll(@NonNull Iterable<T> entities) {
        List<T> result = new ArrayList<>();
        for (T entity : entities) {
            result.add(save(entity));
        }
        return result;
    }

    @Override
    public @NonNull T insert(@NonNull T entity) {
        // 检查实体是否已有ID（应用主动传入）
        Object existingId = queryHelper.getIdValue(entity);
        if (existingId != null) {
            // 使用应用传入的ID直接插入
            String sql = queryHelper.buildInsertWithIdSql();
            List<Object> params = queryHelper.extractInsertWithIdParams(entity);
            dataManager.executeUpdate(sql, params);
            return entity;
        }

        // 没有ID时，从数据库获取生成的主键
        String sql = queryHelper.buildInsertSql();
        List<Object> params = queryHelper.extractInsertParams(entity);
        long generatedId = dataManager.executeInsertAndReturnKey(sql, params);
        queryHelper.setIdValue(entity, generatedId);
        return entity;
    }

    @Override
    public @NonNull List<T> insertAll(@NonNull Iterable<T> entities) {
        // 将 Iterable 转换为 List
        List<T> entityList = new ArrayList<>();
        entities.forEach(entityList::add);

        if (entityList.isEmpty()) {
            return entityList;
        }

        // 分批处理
        List<T> result = new ArrayList<>(entityList.size());
        int totalBatches = (entityList.size() + batchSize - 1) / batchSize;

        for (int batch = 0; batch < totalBatches; batch++) {
            int fromIndex = batch * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, entityList.size());
            List<T> batchEntities = entityList.subList(fromIndex, toIndex);

            // 执行批量插入
            List<T> inserted = executeBatchInsert(batchEntities);
            result.addAll(inserted);
        }

        return result;
    }

    /**
     * 执行批量插入
     *
     * @param entities 当前批次的实体列表（调用方已确保非空）
     * @return 插入后的实体列表（包含生成的主键）
     */
    private List<T> executeBatchInsert(List<T> entities) {
        // 单条记录时，直接使用单条插入以获取生成的主键
        if (entities.size() == 1) {
            return List.of(insert(entities.getFirst()));
        }

        // 对于支持 RETURNING 的数据库（PostgreSQL、H2），使用批量插入返回主键
        if (supportsBatchReturning()) {
            return executeBatchInsertWithReturning(entities);
        }

        // 对于不支持 RETURNING 的数据库，逐条插入
        List<T> result = new ArrayList<>(entities.size());
        for (T entity : entities) {
            result.add(insert(entity));
        }
        return result;
    }

    /**
     * 检查数据库是否支持 INSERT ... RETURNING 语法
     */
    private boolean supportsBatchReturning() {
        return switch (dialect.getDatabaseType()) {
            case POSTGRESQL, OPENGAUSS, GAUSSDB, KINGBASE, H2 -> true;
            default -> false;
        };
    }

    /**
     * 使用 INSERT ... RETURNING 执行批量插入并返回生成的主键
     */
    private <S extends T> List<S> executeBatchInsertWithReturning(List<S> entities) {
        // 分离有ID和无ID的实体
        List<S> withId = new ArrayList<>();
        List<S> withoutId = new ArrayList<>();
        for (S entity : entities) {
            if (getIdValue(entity) != null) {
                withId.add(entity);
            } else {
                withoutId.add(entity);
            }
        }

        List<S> result = new ArrayList<>(entities.size());

        // 处理有ID的实体（直接插入，包含ID字段）
        if (!withId.isEmpty()) {
            for (S entity : withId) {
                String sql = queryHelper.buildInsertWithIdSql();
                List<Object> params = queryHelper.extractInsertWithIdParams(entity);
                dataManager.executeUpdate(sql, params);
                result.add(entity);
            }
        }

        // 处理无ID的实体（获取生成的主键）
        if (!withoutId.isEmpty()) {
            String sql = buildBatchInsertSql(withoutId.size());
            List<Object> params = extractBatchInsertParams(withoutId);
            long[] generatedIds = dataManager.executeBatchInsertAndReturnKeys(sql, params, withoutId.size());
            for (int i = 0; i < withoutId.size(); i++) {
                queryHelper.setIdValue(withoutId.get(i), generatedIds[i]);
            }
            result.addAll(withoutId);
        }

        return result;
    }

    /**
     * 构建批量 INSERT SQL
     * <p>
     * 生成格式：INSERT INTO table (col1, col2, ...) VALUES (?, ?, ...), (?, ?, ...), ...
     *
     * @param batchSize 批次大小
     * @return 批量 INSERT SQL
     */
    private String buildBatchInsertSql(int batchSize) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName())).append(" (");

        // 收集列名（排除自增主键）
        List<String> columns = new ArrayList<>();
        for (var field : metadata.getFields()) {
            if (!field.isGenerated()) {
                columns.add(field.getColumnName());
            }
        }

        // 构建 VALUES 部分
        sql.append(String.join(", ", columns));
        sql.append(") VALUES ");

        // 构建 VALUES 占位符
        String valuePlaceholders = "(" + String.join(", ", Collections.nCopies(columns.size(), "?")) + ")";
        sql.append(String.join(", ", Collections.nCopies(batchSize, valuePlaceholders)));

        return sql.toString();
    }

    /**
     * 提取批量插入参数
     * <p>
     * 使用缓存的字段访问器优化反射性能
     *
     * @param entities 实体列表
     * @return 参数列表（按顺序排列）
     */
    private <S extends T> List<Object> extractBatchInsertParams(List<S> entities) {
        List<Object> params = new ArrayList<>();
        for (S entity : entities) {
            for (var field : metadata.getFields()) {
                if (!field.isGenerated() && field instanceof SimpleFieldMetadata simpleField) {
                    params.add(simpleField.getValue(entity));
                }
            }
        }
        return params;
    }

    @Override
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

    @Override
    public @NonNull List<T> updateAll(@NonNull Iterable<T> entities) {
        List<T> result = new ArrayList<>();
        for (T entity : entities) {
            result.add(update(entity));
        }
        return result;
    }

    @Override
    public @NonNull Optional<T> findById(@NonNull Object id) {
        // 尝试从缓存获取
        if (isCacheEnabled()) {
            EntityCache<T> cache = cacheManager.getCache(entityClass);
            Optional<T> cached = cache.get(id);
            if (cached.isPresent()) {
                return cached;
            }
            // 检查缓存中是否有 null 标记（防穿透）
            if (cache.containsKey(id)) {
                return Optional.empty();
            }
        }

        // 从数据库查询
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()))
                .append(" WHERE id = :id");

        // 自动过滤已删除记录
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, true);
        }

        Optional<T> result = jdbcClient.sql(sqlBuilder.toString())
                .param("id", id)
                .query(rowMapper)
                .optional();

        // 缓存结果
        if (isCacheEnabled()) {
            EntityCache<T> cache = cacheManager.getCache(entityClass);
            result.ifPresentOrElse(
                    entity -> cache.put(id, entity),
                    () -> {
                        // 缓存 null 标记以防止缓存穿透
                        if (cacheManager.getProperties().isCacheNull()) {
                            cache.put(id, null);
                        }
                    }
            );
        }

        return result;
    }

    @Override
    public @NonNull List<T> findAll() {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()));

        // 自动过滤已删除记录
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, false);
        }

        return jdbcClient.sql(sqlBuilder.toString())
                .query(rowMapper)
                .list();
    }

    @Override
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
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, true);
        }

        return jdbcClient.sql(sqlBuilder.toString())
                .param("ids", idList)
                .query(rowMapper)
                .list();
    }

    @Override
    public long count() {
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()));

        // 自动过滤已删除记录
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, false);
        }

        Long result = jdbcClient.sql(sqlBuilder.toString())
                .query(Long.class)
                .single();
        return result != null ? result : 0L;
    }

    @Override
    public boolean existsById(@NonNull Object id) {
        return findById(id).isPresent();
    }

    @Override
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
        evictCacheById(id);
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
        evictCacheById(id);
    }

    @Override
    public void delete(@NonNull T entity) {
        Object id = getIdValue(entity);
        if (id != null) {
            deleteById(id);
        }
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

    @Override
    public @NonNull List<T> findAll(@NonNull Condition condition) {
        ConditionToSqlConverter converter = new ConditionToSqlConverter();
        ConditionToSqlConverter.SqlResult result = converter.convert(condition);

        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()));

        // 构建 WHERE 子句
        StringBuilder whereClause = new StringBuilder(result.sql());

        // 自动过滤已删除记录
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            if (softDeleteHandler.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP) {
                whereClause.append("deleted_at IS NULL");
            } else {
                whereClause.append("deleted = false");
            }
        }

        if (whereClause.length() > 0) {
            sqlBuilder.append(" WHERE ").append(whereClause);
        }

        return jdbcClient.sql(sqlBuilder.toString())
                .params(result.parameters())
                .query(rowMapper)
                .list();
    }

    @Override
    public @NonNull Page<T> findAll(@NonNull Condition condition, @NonNull PageRequest pageable) {
        ConditionToSqlConverter converter = new ConditionToSqlConverter();
        ConditionToSqlConverter.SqlResult whereResult = converter.convert(condition);

        // 构建基础 WHERE 子句
        StringBuilder whereClause = new StringBuilder(whereResult.sql());

        // 自动过滤已删除记录
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            if (softDeleteHandler.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP) {
                if (whereClause.length() > 0) {
                    whereClause.append(" AND ");
                }
                whereClause.append("deleted_at IS NULL");
            } else {
                if (whereClause.length() > 0) {
                    whereClause.append(" AND ");
                }
                whereClause.append("deleted = false");
            }
        }

        // 构建完整 SQL
        String whereSql = whereClause.length() > 0 ? " WHERE " + whereClause : "";

        // 计数查询
        String countSql = "SELECT COUNT(*) FROM " + dialect.quoteIdentifier(metadata.getTableName()) + whereSql;
        long total = dataManager.queryForCount(countSql, whereResult.parameters());

        // 数据查询
        String dataSql = "SELECT * FROM " + dialect.quoteIdentifier(metadata.getTableName()) +
                whereSql +
                " LIMIT " + pageable.size() + " OFFSET " + pageable.offset();
        List<T> records = jdbcClient.sql(dataSql)
                .params(whereResult.parameters())
                .query(rowMapper)
                .list();

        return new Page<>(records, total, pageable.page(), pageable.size());
    }

    @Override
    public long count(@NonNull Condition condition) {
        ConditionToSqlConverter converter = new ConditionToSqlConverter();
        ConditionToSqlConverter.SqlResult result = converter.convert(condition);

        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()));

        // 构建 WHERE 子句
        StringBuilder whereClause = new StringBuilder(result.sql());

        // 自动过滤已删除记录
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            if (softDeleteHandler.getSoftDeleteStrategy() == SoftDeleteStrategy.TIMESTAMP) {
                whereClause.append("deleted_at IS NULL");
            } else {
                whereClause.append("deleted = false");
            }
        }

        if (!whereClause.isEmpty()) {
            sqlBuilder.append(" WHERE ").append(whereClause);
        }

        return dataManager.queryForCount(sqlBuilder.toString(), result.parameters());
    }

    @Override
    public boolean exists(@NonNull Condition condition) {
        return count(condition) > 0;
    }

    @Override
    public @NonNull Optional<T> findOne(@NonNull Condition condition) {
        List<T> results = findAll(condition);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new RuntimeException("Expected one result but got " + results.size());
        }
        return Optional.of(results.get(0));
    }

    @Override
    public @NonNull Optional<T> findFirst(@NonNull Condition condition) {
        ConditionToSqlConverter converter = new ConditionToSqlConverter();
        ConditionToSqlConverter.SqlResult result = converter.convert(condition);

        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ")
                .append(dialect.quoteIdentifier(metadata.getTableName()))
                .append(" WHERE ")
                .append(result.sql());

        // 自动过滤已删除记录
        if (softDeleteHandler.getSoftDeleteStrategy() != null && !includeDeleted) {
            appendSoftDeleteFilter(sqlBuilder, true);
        }

        sqlBuilder.append(" LIMIT 1");

        return jdbcClient.sql(sqlBuilder.toString())
                .params(result.parameters())
                .query(rowMapper)
                .optional();
    }

    // ==================== 条件更新/删除 ====================

    @Override
    public long updateAll(@NonNull Condition condition, @NonNull Map<String, Object> updates) {
        long affected = executeConditionalUpdate(condition, updates);
        if (affected > 0 && isCacheEnabled()) {
            cacheManager.getCacheIfPresent(entityClass).clear();
        }
        return affected;
    }

    private long executeConditionalUpdate(@NonNull Condition condition, @NonNull Map<String, Object> updates) {
        ConditionToSqlConverter converter = new ConditionToSqlConverter();
        ConditionToSqlConverter.SqlResult whereResult = converter.convert(condition);

        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName())).append(" SET ");

        List<String> setParts = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            setParts.add(entry.getKey() + " = ?");
            params.add(entry.getValue());
        }
        sql.append(String.join(", ", setParts));
        sql.append(" WHERE ").append(whereResult.sql());
        params.addAll(whereResult.parameters());

        return dataManager.executeUpdate(sql.toString(), params);
    }

    @Override
    public long deleteAll(@NonNull Condition condition) {
        long affected = executeConditionalDelete(condition);
        if (affected > 0 && isCacheEnabled()) {
            cacheManager.getCacheIfPresent(entityClass).clear();
        }
        return affected;
    }

    private long executeConditionalDelete(@NonNull Condition condition) {
        ConditionToSqlConverter converter = new ConditionToSqlConverter();
        ConditionToSqlConverter.SqlResult result = converter.convert(condition);
        String sql = "DELETE FROM " + dialect.quoteIdentifier(metadata.getTableName()) + " WHERE " + result.sql();
        return dataManager.executeUpdate(sql, result.parameters());
    }

    // ==================== 企业级特性 ====================

    @Override
    public @NonNull EntityProxy<T> withDataScope(@NonNull DataScope scope) {
        this.dataScopes.add(scope);
        return this;
    }

    @Override
    public @NonNull EntityProxy<T> withDataScopes(@NonNull DataScope... scopes) {
        this.dataScopes.addAll(Arrays.asList(scopes));
        return this;
    }

    @Override
    public @NonNull EntityProxy<T> withTenant(@NonNull String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public @NonNull EntityProxy<T> withDataSource(@NonNull String name) {
        this.dataSourceName = name;
        return this;
    }

    @Override
    public @NonNull EntityProxy<T> withReadOnly() {
        this.readOnly = true;
        return this;
    }

    @Override
    public @NonNull EntityProxy<T> includeDeleted() {
        this.includeDeleted = true;
        return this;
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
        this.batchSize = batchSize;
        return this;
    }

    /**
     * 获取当前批次大小
     *
     * @return 批次大小
     */
    public int getBatchSize() {
        return batchSize;
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

    @Override
    public @NonNull EntityProxy<T> withAssociation(@NonNull String name) {
        validateAssociation(name);
        eagerFetchAssociations.add(name);
        return this;
    }

    @Override
    public @NonNull EntityProxy<T> withAssociations(@NonNull String... names) {
        for (String name : names) {
            validateAssociation(name);
            eagerFetchAssociations.add(name);
        }
        return this;
    }

    /**
     * 验证关联字段是否存在
     *
     * @param name 关联字段名
     * @throws IllegalArgumentException 如果关联字段不存在
     */
    private void validateAssociation(String name) {
        if (!metadata.hasRelation(name)) {
            throw new IllegalArgumentException(
                    "Association '" + name + "' not found in entity " + entityClass.getSimpleName()
            );
        }
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
            case ONE_TO_MANY:
                associationLoader.fetchAllOneToMany(ids, relation, targetEntityClass);
                break;
            case MANY_TO_MANY:
                associationLoader.fetchAllManyToMany(ids, relation, targetEntityClass);
                break;
        }
    }

    @Override
    public @NonNull EntityProxy<T> clearAssociations() {
        eagerFetchAssociations.clear();
        return this;
    }

    /**
     * 获取要急加载的关联字段集合
     *
     * @return 关联字段集合
     */
    public Set<String> getEagerFetchAssociations() {
        return Collections.unmodifiableSet(eagerFetchAssociations);
    }

    /**
     * 获取 ID 值
     */
    private @Nullable Object getIdValue(T entity) {
        return queryHelper.getIdValue(entity);
    }

    // ==================== 缓存辅助方法 ====================

    /**
     * 检查缓存是否启用
     *
     * @return 启用返回 true
     */
    private boolean isCacheEnabled() {
        return cacheManager != null && cacheManager.isEnabled();
    }

    /**
     * 失效实体缓存
     *
     * @param entity 实体对象
     */
    private void evictCache(@NonNull T entity) {
        if (!isCacheEnabled()) {
            return;
        }
        Object id = queryHelper.getIdValue(entity);
        if (id != null) {
            evictCacheById(id);
        }
    }

    /**
     * 根据 ID 失效缓存
     *
     * @param id 实体 ID
     */
    private void evictCacheById(@NonNull Object id) {
        if (!isCacheEnabled()) {
            return;
        }
        EntityCache<T> cache = cacheManager.getCacheIfPresent(entityClass);
        if (cache != null) {
            cache.evict(id);
        }
    }

    /**
     * 获取缓存管理器
     *
     * @return 缓存管理器，可能为 null
     */
    public @Nullable EntityCacheManager getCacheManager() {
        return cacheManager;
    }

    // ==================== 软删除辅助方法 ====================

    /**
     * 追加软删除过滤条件到 SQL
     *
     * @param sqlBuilder     SQL 构建器
     * @param hasWhereClause 是否已有 WHERE 子句
     */
    private void appendSoftDeleteFilter(StringBuilder sqlBuilder, boolean hasWhereClause) {
        softDeleteHandler.appendSoftDeleteFilter(sqlBuilder, hasWhereClause, includeDeleted);
    }
}
