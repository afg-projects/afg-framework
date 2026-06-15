package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.entity.*;
import io.github.afgprojects.framework.data.core.encryption.BlindIndexProvider;
import io.github.afgprojects.framework.data.core.exception.OptimisticLockException;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
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
@Slf4j
public class EntityUpdateHandler<T> {

    private final Class<T> entityClass;
    private final JdbcClient jdbcClient;
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
     * 盲索引提供者（可注入，用于加密字段的盲索引值计算）
     */
    private BlindIndexProvider blindIndexProvider;

    /**
     * 保存加密前的明文值，用于盲索引计算
     */
    private static final ThreadLocal<java.util.Map<String, String>> PLAINTEXT_CACHE = new ThreadLocal<>();

    public EntityUpdateHandler(Class<T> entityClass, JdbcClient jdbcClient,
                               EntityMetadata<T> metadata, EntityQueryHelper<T> queryHelper,
                               JdbcDataManager dataManager, EntityCacheHandler<T> cacheHandler) {
        this.entityClass = entityClass;
        this.jdbcClient = jdbcClient;
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
     * 设置盲索引提供者
     * <p>
     * 注入与 AutoConfiguration 创建的同一实例，
     * 确保盲索引计算在 INSERT/UPDATE 中使用相同的密钥。
     * 如果不设置，不计算盲索引值（向后兼容）。
     *
     * @param blindIndexProvider 盲索引提供者
     */
    public void setBlindIndexProvider(@Nullable BlindIndexProvider blindIndexProvider) {
        this.blindIndexProvider = blindIndexProvider;
    }

    /**
     * 更新单个实体
     *
     * @param entity 实体对象
     * @return 更新后的实体
     * @throws OptimisticLockException 如果乐观锁检查失败
     */
    public @NonNull T update(@NonNull T entity) {
        // 自动刷新 updatedAt 时间戳
        autoFillUpdatedAt(entity);

        // 自动填充 updateBy 审计字段
        autoFillUpdateBy(entity);

        // 重新计算树形路径（当 parentId 变化时）
        autoFillTreePath(entity);

        // 加密字段：在 SQL UPDATE 之前对加密字段执行加密
        encryptFields(entity);

        boolean isVersioned = Versioned.class.isAssignableFrom(entityClass);
        String sql = queryHelper.buildUpdateSql(isVersioned);
        List<Object> params = queryHelper.extractUpdateParams(entity, isVersioned);
        sql = augmentSqlWithBlindIndexColumns(sql, params);
        int affectedRows = dataManager.executeUpdate(sql, params);
        clearBlindIndexCache();
        // 解密字段：UPDATE 后将实体中的密文恢复为明文
        decryptFields(entity);

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

        // 触发 afterUpdate 生命周期回调（类似 JPA @PostUpdate）
        LifecycleCallbacks.ifCallback(entity, cb -> cb.afterUpdate(entity));

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

    /**
     * 自动刷新 updatedAt 时间戳。
     *
     * <p>当实体具有 TIMESTAMPED 特征时，自动将 updatedAt 设置为当前时间。
     *
     * @param entity 实体对象
     */
    private void autoFillUpdatedAt(T entity) {
        if (!metadata.hasTrait(EntityTrait.TIMESTAMPED)) {
            return;
        }
        queryHelper.getParameterExtractor().setFieldValue(entity, "updatedAt", Instant.now());
    }

    /**
     * 自动填充 updateBy 审计字段。
     *
     * <p>当实体具有 AUDITABLE 特征且 {@link AuditableContext#getCurrentUserId()} 不为 null 时，
     * 无条件将 updateBy 设置为当前用户 ID（与 insert 不同，update 始终覆盖）。
     *
     * @param entity 实体对象
     */
    private void autoFillUpdateBy(T entity) {
        if (!metadata.hasTrait(EntityTrait.AUDITABLE)) {
            return;
        }
        String currentUserId = auditableContext.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }
        queryHelper.getParameterExtractor().setFieldValue(entity, "updateBy", currentUserId);
    }

    /**
     * 加密实体中的加密字段。
     *
     * <p>当实体具有 ENCRYPTED 特征时，遍历所有加密字段，
     * 对非 null 的字段值调用 {@link FieldEncryptor#encrypt(String, String, String)} 进行加密，
     * 然后将加密后的密文设置回实体。此操作在 SQL UPDATE 之前执行，
     * 确保数据库中存储的是密文。
     *
     * @param entity 实体对象
     */
    private void encryptFields(T entity) {
        if (!metadata.hasTrait(EntityTrait.ENCRYPTED)) {
            return;
        }
        var extractor = queryHelper.getParameterExtractor();
        java.util.Map<String, String> plaintextCache = new java.util.HashMap<>();
        for (EncryptedFieldMetadata encryptedField : metadata.getEncryptedFields()) {
            Object value = extractor.getFieldValue(entity, encryptedField.fieldName());
            if (value != null) {
                String plaintext = value.toString();
                // 缓存明文用于盲索引计算
                if (encryptedField.hasBlindIndex() && blindIndexProvider != null) {
                    plaintextCache.put(encryptedField.fieldName(), plaintext);
                }
                String ciphertext = fieldEncryptor.encrypt(plaintext, encryptedField.algorithm(), encryptedField.keyRef());
                extractor.setFieldValue(entity, encryptedField.fieldName(), ciphertext);
            }
        }
        if (!plaintextCache.isEmpty()) {
            PLAINTEXT_CACHE.set(plaintextCache);
        }
    }

    /**
     * 解密实体中的加密字段。
     * <p>
     * UPDATE 后调用此方法将实体中的密文恢复为明文，
     * 确保调用方拿到的实体对象持有原始值而非密文。
     *
     * @param entity 实体对象
     */
    private void decryptFields(T entity) {
        if (!metadata.hasTrait(EntityTrait.ENCRYPTED)) {
            return;
        }
        var extractor = queryHelper.getParameterExtractor();
        for (EncryptedFieldMetadata encryptedField : metadata.getEncryptedFields()) {
            Object value = extractor.getFieldValue(entity, encryptedField.fieldName());
            if (value != null) {
                String ciphertext = value.toString();
                String plaintext = fieldEncryptor.decrypt(ciphertext, encryptedField.algorithm(), encryptedField.keyRef());
                extractor.setFieldValue(entity, encryptedField.fieldName(), plaintext);
            }
        }
    }

    /**
     * 重新计算树形实体的 level 和 path 字段。
     *
     * <p>当实体具有 TREEABLE 特征且 parentId 发生变化时，重新计算 level 和 path：
     * <ul>
     *   <li>parentId 为 null（变为根节点）：level=1, path="/"</li>
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
            }
            // 如果无法查询到父节点，保留当前的 level 和 path 不变
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

    /**
     * 在 UPDATE SQL 中添加盲索引列和参数
     * <p>
     * 读取 PLAINTEXT_CACHE 中缓存的明文值，计算 HMAC 盲索引值，
     * 修改 UPDATE SQL 以包含盲索引列，并将盲索引值追加到参数列表。
     *
     * @param sql    UPDATE SQL 字符串
     * @param params UPDATE 参数列表
     * @return 修改后的 UPDATE SQL
     */
    private String augmentSqlWithBlindIndexColumns(String sql, List<Object> params) {
        java.util.Map<String, String> plaintextCache = PLAINTEXT_CACHE.get();
        if (plaintextCache == null || plaintextCache.isEmpty() || blindIndexProvider == null) {
            return sql;
        }

        // 格式: "UPDATE `table` SET col1=?, col2=? WHERE id=?"
        int setIdx = sql.indexOf(" SET ");
        int whereIdx = sql.indexOf(" WHERE ");
        if (setIdx < 0 || whereIdx < 0) {
            return sql;
        }

        StringBuilder augmentedSql = new StringBuilder();
        // 找到 SET 子句的结束位置（在 WHERE 之前）
        String setClause = sql.substring(setIdx + 5, whereIdx).trim();
        String beforeSet = sql.substring(0, setIdx + 5);
        String afterSet = sql.substring(whereIdx);

        augmentedSql.append(beforeSet).append(setClause);

        // 收集盲索引列和值
        java.util.List<String> blindIndexColumns = new java.util.ArrayList<>();
        java.util.List<String> blindIndexValues = new java.util.ArrayList<>();
        java.util.List<EncryptedFieldMetadata> encryptedFields = metadata.getEncryptedFields();
        for (EncryptedFieldMetadata efm : encryptedFields) {
            String plaintext = plaintextCache.get(efm.fieldName());
            if (plaintext != null && efm.hasBlindIndex()) {
                blindIndexColumns.add(queryHelper.getDialect().quoteIdentifier(efm.blindIndexColumn()) + "=?");
                String blindIndex = blindIndexProvider.computeBlindIndex(
                    plaintext, efm.fieldName(), efm.keyRef());
                blindIndexValues.add(blindIndex);
            }
        }

        if (blindIndexColumns.isEmpty()) {
            return sql;
        }

        // 追加盲索引列到 SET 子句
        augmentedSql.append(", ").append(String.join(", ", blindIndexColumns));
        augmentedSql.append(" ").append(afterSet.trim());

        // 盲索引值需要插入到 WHERE 参数之前（id 参数之前）
        // params 顺序: [SET_col1, SET_col2, ..., WHERE_id]
        // 盲索引值应插入在 WHERE_id 之前
        int insertPos = params.size() - 1; // 在最后一个参数（id）之前
        params.addAll(insertPos, blindIndexValues);

        return augmentedSql.toString();
    }

    /**
     * 清除盲索引明文缓存
     */
    private void clearBlindIndexCache() {
        PLAINTEXT_CACHE.remove();
    }
}
