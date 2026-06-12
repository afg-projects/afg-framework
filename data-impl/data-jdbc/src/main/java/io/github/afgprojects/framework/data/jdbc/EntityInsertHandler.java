package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.entity.*;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
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
@Slf4j
public class EntityInsertHandler<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;
    private final EntityQueryHelper<T> queryHelper;
    private final JdbcDataManager dataManager;
    private final EntityCacheHandler<T> cacheHandler;

    /**
     * 审计上下文（可注入，支持与 AuditableContext Bean 共享）
     */
    private AuditableContext auditableContext;

    /**
     * 字段加密器（可注入，支持与 FieldEncryptor Bean 共享）
     */
    private FieldEncryptor fieldEncryptor;

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
        this.auditableContext = new NoOpAuditableContext();
        this.fieldEncryptor = new NoOpFieldEncryptor();
    }

    /**
     * 设置审计上下文
     * <p>
     * 注入与 AutoConfiguration 创建的同一实例，
     * 确保审计用户信息在整个应用中一致。
     * 如果不设置，使用构造函数中创建的 NoOp 默认实例。
     *
     * @param auditableContext 审计上下文
     */
    public void setAuditableContext(@NonNull AuditableContext auditableContext) {
        this.auditableContext = auditableContext;
    }

    /**
     * 设置字段加密器
     * <p>
     * 注入与 AutoConfiguration 创建的同一实例，
     * 确保加密/解密行为在整个应用中一致。
     * 如果不设置，使用构造函数中创建的 NoOp 默认实例（不加密）。
     *
     * @param fieldEncryptor 字段加密器
     */
    public void setFieldEncryptor(@NonNull FieldEncryptor fieldEncryptor) {
        this.fieldEncryptor = fieldEncryptor;
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

        // 自动填充 createdAt / updatedAt 时间戳
        autoFillTimestamps(entity, true);

        // 自动填充 createBy / updateBy 审计字段
        autoFillAuditable(entity);

        // 自动填充树形路径（level + path）
        autoFillTreePath(entity);

        // 自动填充租户ID：当实体的租户字段为空且 TenantContextHolder 设置了租户ID时
        var tenantField = metadata.getTenantField();
        if (tenantField != null) {
            String currentTenantId = dataManager.getTenantContextHolder().getTenantId();
            if (currentTenantId != null) {
                Object entityTenantId = queryHelper.getParameterExtractor().getFieldValue(entity, tenantField.getPropertyName());
                if (entityTenantId == null) {
                    queryHelper.getParameterExtractor().setFieldValue(entity, tenantField.getPropertyName(), currentTenantId);
                }
            }
        }

        // 加密字段：在 SQL INSERT 之前对加密字段执行加密
        encryptFields(entity);

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
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "batchSize must be greater than 0");
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

    /**
     * 自动填充 createdAt / updatedAt 时间戳。
     *
     * <p>当实体具有 TIMESTAMPED 特征时，如果 createdAt 或 updatedAt 为 null，
     * 自动填充为当前时间。对于 insert 操作，两个字段都会填充；
     * 对于 update 操作，仅填充 updatedAt。
     *
     * @param entity  实体对象
     * @param isInsert 是否为插入操作
     */
    private void autoFillTimestamps(T entity, boolean isInsert) {
        if (!metadata.hasTrait(EntityTrait.TIMESTAMPED)) {
            return;
        }
        Instant now = Instant.now();
        var extractor = queryHelper.getParameterExtractor();

        if (isInsert) {
            Object createdAt = extractor.getFieldValue(entity, "createdAt");
            if (createdAt == null) {
                extractor.setFieldValue(entity, "createdAt", now);
            }
        }

        Object updatedAt = extractor.getFieldValue(entity, "updatedAt");
        if (updatedAt == null) {
            extractor.setFieldValue(entity, "updatedAt", now);
        }
    }

    /**
     * 自动填充 createBy / updateBy 审计字段。
     *
     * <p>当实体具有 AUDITABLE 特征且 {@link AuditableContext#getCurrentUserId()} 不为 null 时，
     * 如果 createBy 或 updateBy 为 null，自动填充为当前用户 ID。
     *
     * @param entity 实体对象
     */
    private void autoFillAuditable(T entity) {
        if (!metadata.hasTrait(EntityTrait.AUDITABLE)) {
            return;
        }
        String currentUserId = auditableContext.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }
        var extractor = queryHelper.getParameterExtractor();

        Object createBy = extractor.getFieldValue(entity, "createBy");
        if (createBy == null) {
            extractor.setFieldValue(entity, "createBy", currentUserId);
        }

        Object updateBy = extractor.getFieldValue(entity, "updateBy");
        if (updateBy == null) {
            extractor.setFieldValue(entity, "updateBy", currentUserId);
        }
    }

    /**
     * 加密实体中的加密字段。
     *
     * <p>当实体具有 ENCRYPTED 特征时，遍历所有加密字段，
     * 对非 null 的字段值调用 {@link FieldEncryptor#encrypt(String, String, String)} 进行加密，
     * 然后将加密后的密文设置回实体。此操作在 SQL INSERT 之前执行，
     * 确保数据库中存储的是密文。
     *
     * @param entity 实体对象
     */
    private void encryptFields(T entity) {
        if (!metadata.hasTrait(EntityTrait.ENCRYPTED)) {
            return;
        }
        var extractor = queryHelper.getParameterExtractor();
        for (EncryptedFieldMetadata encryptedField : metadata.getEncryptedFields()) {
            Object value = extractor.getFieldValue(entity, encryptedField.fieldName());
            if (value != null) {
                String plaintext = value.toString();
                String ciphertext = fieldEncryptor.encrypt(plaintext, encryptedField.algorithm(), encryptedField.keyRef());
                extractor.setFieldValue(entity, encryptedField.fieldName(), ciphertext);
            }
        }
    }

    /**
     * 自动填充树形实体的 level 和 path 字段。
     *
     * <p>当实体具有 TREEABLE 特征时，根据 parentId 自动计算层级和路径：
     * <ul>
     *   <li>parentId 为 null（根节点）：level=1, path="/"</li>
     *   <li>parentId 不为 null（子节点）：查询父节点，level=parent.level+1, path=parent.path+parent.id+"/"</li>
     * </ul>
     *
     * @param entity 实体对象
     */
    @SuppressWarnings("unchecked")
    private void autoFillTreePath(T entity) {
        if (!metadata.hasTrait(EntityTrait.TREEABLE)) {
            return;
        }
        if (!(entity instanceof Treeable<?> treeable)) {
            return;
        }

        Long parentId = treeable.getParentId();
        if (parentId == null) {
            // 根节点
            treeable.setLevel(1);
            treeable.setPath("/");
        } else {
            // 子节点：查询父节点以计算 level 和 path
            T parentEntity = findParentEntity(parentId);
            if (parentEntity instanceof Treeable<?> parent) {
                int parentLevel = parent.getLevel() != null ? parent.getLevel() : 1;
                String parentPath = parent.getPath() != null ? parent.getPath() : "/";
                Long parentIdValue = parentEntity instanceof BaseEntity be ? be.getId() : parentId;
                treeable.setLevel(parentLevel + 1);
                treeable.setPath(parentPath + parentIdValue + "/");
            } else {
                // 无法查询到父节点，使用保守默认值
                log.warn("Parent entity not found for tree entity {} with parentId={}, " +
                         "using default level=2 and path=/{}/", entityClass.getSimpleName(), parentId, parentId);
                treeable.setLevel(2);
                treeable.setPath("/" + parentId + "/");
            }
        }
    }

    /**
     * 查询父实体
     *
     * @param parentId 父节点 ID
     * @return 父实体，可能为 null
     */
    private @Nullable T findParentEntity(Long parentId) {
        try {
            return dataManager.entity(entityClass).findById(parentId).orElse(null);
        } catch (Exception e) {
            log.debug("Failed to find parent entity with id={} for tree entity {}: {}",
                      parentId, entityClass.getSimpleName(), e.getMessage());
            return null;
        }
    }
}
