package io.github.afgprojects.framework.data.core.entity;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * NoOp 字段加密实现（降级）
 * <p>
 * 直接返回原文，不执行任何加密/解密操作。作为框架的默认降级实现，
 * 确保不引入外部加密依赖时框架仍可正常运行。
 *
 * <p><b>安全警告：</b>此实现将敏感数据以明文形式存储在数据库中，
 * 存在严重的数据泄露风险。生产环境必须提供自定义 {@link FieldEncryptor} 实现
 * （如基于 AES、KMS 或 HSM 的加密方案），否则使用 {@code @EncryptedField} 注解的字段
 * 将以明文形式存储。
 *
 * <p>当业务应用注册了自定义 {@link FieldEncryptor} 实现后，
 * 此 NoOp 实现通过 {@code @ConditionalOnMissingBean} 自动退让。
 *
 * @see FieldEncryptor
 */
@Slf4j
public class NoOpFieldEncryptor implements FieldEncryptor {

    private static final AtomicBoolean warned = new AtomicBoolean(false);

    @Override
    public String encrypt(String plaintext, String algorithm, @Nullable String keyRef) {
        if (warned.compareAndSet(false, true)) {
            log.warn("Field encryption is DISABLED! Sensitive data annotated with @EncryptedField "
                    + "will be stored in PLAINTEXT. This is a serious security risk in production. "
                    + "Implement a FieldEncryptor bean (e.g., AES-based) to enable real field encryption.");
        }
        return plaintext;
    }

    @Override
    public String decrypt(String ciphertext, String algorithm, @Nullable String keyRef) {
        if (warned.compareAndSet(false, true)) {
            log.warn("Field encryption is DISABLED! Sensitive data annotated with @EncryptedField "
                    + "will be stored in PLAINTEXT. This is a serious security risk in production. "
                    + "Implement a FieldEncryptor bean (e.g., AES-based) to enable real field encryption.");
        }
        return ciphertext;
    }
}
