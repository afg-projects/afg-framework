package io.github.afgprojects.framework.core.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 配置加密器接口
 * 用于敏感配置的加密和解密
 */
public interface ConfigEncryptor {

    /**
     * 加密明文
     *
     * @param plaintext 明文
     * @return 密文（包含前缀和后缀）
     */
    @NonNull String encrypt(@NonNull String plaintext);

    /**
     * 解密密文
     *
     * @param ciphertext 密文（包含前缀和后缀）
     * @return 明文
     * @throws IllegalArgumentException 如果密文格式无效
     */
    @NonNull String decrypt(@NonNull String ciphertext);

    /**
     * 获取加密值的前缀
     * 例如 "ENC("
     */
    @NonNull String prefix();

    /**
     * 获取加密值的后缀
     * 例如 ")"
     */
    @NonNull String suffix();

    /**
     * 判断值是否已加密
     *
     * @param value 配置值
     * @return 如果已加密返回 true
     */
    default boolean isEncrypted(@Nullable String value) {
        if (value == null) {
            return false;
        }
        return value.startsWith(prefix()) && value.endsWith(suffix());
    }

    /**
     * 提取密文内容（移除前缀和后缀）
     *
     * @param value 包含前缀和后缀的加密值
     * @return 密文内容
     */
    default String extractContent(@NonNull String value) {
        int prefixLen = prefix().length();
        int suffixLen = suffix().length();
        return value.substring(prefixLen, value.length() - suffixLen);
    }

    /**
     * 如果值已加密则解密，否则返回原值
     *
     * @param value 配置值
     * @return 解密后的值或原值
     */
    default String decryptIfNeeded(@Nullable String value) {
        if (!isEncrypted(value)) {
            return value;
        }
        return decrypt(value);
    }
}
