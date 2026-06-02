package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 实体插入操作处理器
 * <p>
 * 封装实体插入相关的逻辑，包括单条插入、批量插入等操作。
 * 从 JdbcEntityProxy 中提取，降低类的复杂度。
 *
 * @param <T> 实体类型
 */
public class EntityInsertHandler<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;
    private final EntityQueryHelper<T> queryHelper;
    private final JdbcDataManager dataManager;
    private final EntityCacheHandler<T> cacheHandler;

    /**
     * 默认批次大小（使用 volatile 确保多线程可见性）
     */
    private volatile int batchSize = 1000;

    public EntityInsertHandler(Class<T> entityClass, JdbcClient jdbcClient, Dialect dialect,
                               EntityMetadata<T> metadata, EntityQueryHelper<T> queryHelper,
                               JdbcDataManager dataManager, EntityCacheHandler<T> cacheHandler) {
        this.entityClass = entityClass;
        this.jdbcClient = jdbcClient;
        this.dialect = dialect;
        this.metadata = metadata;
        this.queryHelper = queryHelper;
        this.dataManager = dataManager;
        this.cacheHandler = cacheHandler;
    }

    /**
     * 插入单个实体
     *
     * @param entity 实体对象
     * @return 插入后的实体（包含生成的主键）
     */
    public @NonNull T insert(@NonNull T entity) {
        // 触发 beforeCreate 生命周期回调（类似 JPA @PrePersist）
        LifecycleCallbacks.ifCallback(entity, LifecycleCallbacks::beforeCreate);

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

    /**
     * 批量插入实体
     *
     * @param entities 实体集合
     * @return 插入后的实体列表（包含生成的主键）
     */
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
     * 设置批量插入的批次大小
     *
     * @param batchSize 批次大小，必须大于 0
     */
    public void setBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than 0");
        }
        this.batchSize = batchSize;
    }

    /**
     * 获取当前批次大小
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 执行批量插入
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
    @SuppressWarnings("unchecked")
    private <S extends T> List<S> executeBatchInsertWithReturning(List<S> entities) {
        // 分离有ID和无ID的实体
        List<S> withId = new ArrayList<>();
        List<S> withoutId = new ArrayList<>();
        for (S entity : entities) {
            if (queryHelper.getIdValue(entity) != null) {
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
     */
    @SuppressWarnings("unchecked")
    private <S extends T> List<Object> extractBatchInsertParams(List<S> entities) {
        List<Object> params = new ArrayList<>();
        for (S entity : entities) {
            params.addAll(queryHelper.extractInsertParams(entity));
        }
        return params;
    }
}
