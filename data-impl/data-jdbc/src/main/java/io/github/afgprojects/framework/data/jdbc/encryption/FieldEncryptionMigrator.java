package io.github.afgprojects.framework.data.jdbc.encryption;

import io.github.afgprojects.framework.data.core.encryption.BlindIndexProvider;
import io.github.afgprojects.framework.data.core.entity.EncryptedFieldMetadata;
import io.github.afgprojects.framework.data.core.entity.FieldEncryptor;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * 字段加密迁移工具
 * <p>
 * 将指定实体的指定字段从明文迁移为密文。
 * 这是一个工具类，由开发者手动调用（不在应用启动时自动执行）。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * @Autowired
 * JdbcDataManager dataManager;
 *
 * JdbcClient jdbcClient = dataManager.getJdbcClient();
 * var migrator = new FieldEncryptionMigrator(jdbcClient, encryptor, blindIndexProvider);
 * migrator.migrateToEncrypted(jdbcClient, entityClass, "phone");
 * }</pre>
 *
 * <h3>迁移流程</h3>
 * <ol>
 *   <li>读取所有记录的明文值</li>
 *   <li>计算盲索引值</li>
 *   <li>加密明文值</li>
 *   <li>批量 UPDATE：SET field = ciphertext, field_blind_idx = hmac_value</li>
 * </ol>
 */
@Slf4j
public class FieldEncryptionMigrator {

    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexProvider blindIndexProvider;

    /**
     * 创建字段加密迁移工具
     *
     * @param fieldEncryptor      字段加密器
     * @param blindIndexProvider  盲索引提供者
     */
    public FieldEncryptionMigrator(@NonNull FieldEncryptor fieldEncryptor,
                                   @NonNull BlindIndexProvider blindIndexProvider) {
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexProvider = blindIndexProvider;
    }

    /**
     * 将指定实体的指定字段从明文迁移为密文
     * <p>
     * 分批迁移（每批 1000 条），避免大事务。
     *
     * @param jdbcClient     JDBC 客户端
     * @param entityClass    实体类
     * @param metadata       实体元数据
     * @param fieldName      字段名（Java 属性名）
     * @param tableName      表名
     * @param columnName     列名
     * @param batchSize      批次大小
     * @return 迁移的记录数
     */
    public int migrateToEncrypted(@NonNull JdbcClient jdbcClient,
                                   @NonNull Class<?> entityClass,
                                   @NonNull EntityMetadata<?> metadata,
                                   @NonNull String fieldName,
                                   @NonNull String tableName,
                                   @NonNull String columnName,
                                   int batchSize) {
        if (!metadata.hasTrait(EntityTrait.ENCRYPTED)) {
            throw new IllegalArgumentException(
                "Entity " + entityClass.getSimpleName() + " does not have @EncryptedField annotations");
        }

        EncryptedFieldMetadata efm = metadata.getEncryptedField(fieldName);
        if (efm == null) {
            throw new IllegalArgumentException(
                "Field '" + fieldName + "' is not marked with @EncryptedField on entity "
                + entityClass.getSimpleName());
        }

        if (!efm.hasBlindIndex()) {
            throw new IllegalArgumentException(
                "Field '" + fieldName + "' does not have a blind index column configured. "
                + "Cannot migrate without blind indexing.");
        }

        String blindIndexColumn = efm.blindIndexColumn();
        int totalMigrated = 0;
        int offset = 0;

        while (true) {
            // 分批读取明文
            String selectSql = "SELECT " + columnName + " FROM " + tableName + " LIMIT ? OFFSET ?";
            var rows = jdbcClient.sql(selectSql)
                .param(batchSize)
                .param(offset)
                .query((rs, rowNum) -> rs.getString(columnName))
                .list();

            if (rows.isEmpty()) {
                break;
            }

            for (String plaintext : rows) {
                if (plaintext == null) {
                    continue;
                }
                String ciphertext = fieldEncryptor.encrypt(plaintext, efm.algorithm(), efm.keyRef());
                String blindIndex = blindIndexProvider.computeBlindIndex(
                    plaintext, efm.fieldName(), efm.keyRef());

                String updateSql = "UPDATE " + tableName + " SET "
                    + columnName + " = ?, " + blindIndexColumn + " = ?"
                    + " WHERE " + columnName + " = ?";
                jdbcClient.sql(updateSql)
                    .param(ciphertext)
                    .param(blindIndex)
                    .param(plaintext)
                    .update();

                totalMigrated++;
            }

            offset += batchSize;
            log.info("Migrated {} records for field {}.{} (offset: {})",
                     rows.size(), entityClass.getSimpleName(), fieldName, offset - batchSize);
        }

        log.info("Encryption migration complete for {}.{}: {} records migrated.",
                 entityClass.getSimpleName(), fieldName, totalMigrated);
        return totalMigrated;
    }
}
