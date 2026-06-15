package io.github.afgprojects.framework.data.jdbc.encryption;

import io.github.afgprojects.framework.data.core.entity.FieldEncryptor;
import io.github.afgprojects.framework.data.core.entity.NoOpFieldEncryptor;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 加密严格模式检查器
 * <p>
 * 在严格模式下（默认），当存在 {@code @EncryptedField} 标注的实体
 * 但当前 {@link FieldEncryptor} 是 {@link NoOpFieldEncryptor} 时，
 * 记录 ERROR 日志警告。
 *
 * <p>严格模式检查发生在应用启动后，因此它更适合作为调试/监控工具而不是启动守护。
 * 密钥无效的情况会在 {@link ConfigFieldEncryptionKeyProvider} 和
 * {@link AesGcmFieldEncryptor} 构造时立即抛出异常来保证。
 *
 * <p>严格模式可通过 {@code afg.data.encryption.strict-mode=false} 关闭。
 */
@Slf4j
public class EncryptionStrictModeChecker {

    private final JdbcDataManager dataManager;
    private final boolean strictMode;

    public EncryptionStrictModeChecker(JdbcDataManager dataManager, boolean strictMode) {
        this.dataManager = dataManager;
        this.strictMode = strictMode;
    }

    /**
     * 检查当前 FieldEncryptor 是否为 NoOp
     */
    public void check() {
        FieldEncryptor fieldEncryptor = dataManager.getFieldEncryptor();
        if (!(fieldEncryptor instanceof NoOpFieldEncryptor)) {
            log.info("Field encryption is active using {}", fieldEncryptor.getClass().getSimpleName());
            return;
        }

        String message = "FieldEncryptor is still NoOpFieldEncryptor. "
            + "No real field encryption is active. "
            + "Set afg.data.encryption.strict-mode=false to suppress this message.";

        if (strictMode) {
            log.error("SECURITY WARNING: {}", message);
        } else {
            log.info("{} (strict-mode disabled)", message);
        }
    }
}
