package io.github.afgprojects.framework.data.jdbc.encryption;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.encryption.FieldEncryptionKeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于配置文件的密钥提供者
 * <p>
 * 从 Spring 配置 {@code afg.data.encryption.keys.<keyRef>} 读取 Base64 编码的密钥。
 * 当 keyRef 为空时使用 {@code afg.data.encryption.default-key}。
 *
 * <h3>密钥派生</h3>
 * <p>
 * 加密密钥直接使用配置的 Base64 解码值。
 * 盲索引密钥通过 HMAC-SHA256 从加密密钥派生，使用固定的派生后缀 {@code ":blind:"}，
 * 确保加密密钥和盲索引密钥分离。
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   data:
 *     encryption:
 *       default-key: "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="  # Base64 of 32 bytes
 *       keys:
 *         user-key: "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="
 *         phone-key: "eW91ci1iYXNlNjQtZW5jb2RlZC0zMi1ieXRlLWtleQ=="
 * </pre>
 */
@Slf4j
public class ConfigFieldEncryptionKeyProvider implements FieldEncryptionKeyProvider {

    private static final int AES_KEY_LENGTH = 32; // AES-256
    private static final String BLIND_INDEX_DERIVE_SUFFIX = ":blind:";

    private final String defaultKey;
    private final ConcurrentHashMap<String, byte[]> encryptionKeyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, byte[]> blindIndexKeyCache = new ConcurrentHashMap<>();
    private final java.util.Map<String, String> keys;

    /**
     * 创建基于配置的密钥提供者
     *
     * @param properties 加密配置属性
     */
    public ConfigFieldEncryptionKeyProvider(EncryptionProperties properties) {
        this.defaultKey = properties.getDefaultKey();
        this.keys = properties.getKeys();
        validateDefaultKey();
    }

    @Override
    public byte[] getEncryptionKey(@Nullable String keyRef) {
        String effectiveKeyRef = resolveKeyRef(keyRef);
        return encryptionKeyCache.computeIfAbsent(effectiveKeyRef, this::resolveEncryptionKey);
    }

    @Override
    public byte[] getBlindIndexKey(@Nullable String keyRef) {
        String effectiveKeyRef = resolveKeyRef(keyRef);
        return blindIndexKeyCache.computeIfAbsent(effectiveKeyRef, this::deriveBlindIndexKey);
    }

    /**
     * 解析有效的密钥引用名称
     * <p>
     * keyRef 为空或 null 时使用 "default"
     */
    private String resolveKeyRef(@Nullable String keyRef) {
        return (keyRef == null || keyRef.isEmpty()) ? "default" : keyRef;
    }

    /**
     * 解析加密密钥
     */
    private byte[] resolveEncryptionKey(String keyRef) {
        String base64Key;
        if ("default".equals(keyRef)) {
            base64Key = defaultKey;
        } else {
            base64Key = keys.get(keyRef);
        }

        if (base64Key == null || base64Key.isEmpty()) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_KEY_INVALID,
                "Encryption key not found for keyRef '" + keyRef + "'. "
                + "Configure it via afg.data.encryption.keys." + keyRef + " or afg.data.encryption.default-key");
        }

        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_KEY_INVALID,
                "Encryption key for keyRef '" + keyRef + "' is not valid Base64: " + e.getMessage());
        }

        if (key.length != AES_KEY_LENGTH) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_KEY_INVALID,
                "Encryption key for keyRef '" + keyRef + "' must be " + AES_KEY_LENGTH
                + " bytes (AES-256), but was " + key.length + " bytes");
        }

        return key;
    }

    /**
     * 从加密密钥派生盲索引密钥
     * <p>
     * 使用 HMAC-SHA256(encryptionKey, BLIND_INDEX_DERIVE_SUFFIX) 确保密钥分离
     */
    private byte[] deriveBlindIndexKey(String keyRef) {
        byte[] encryptionKey = resolveEncryptionKey(keyRef);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(BLIND_INDEX_DERIVE_SUFFIX.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_ERROR,
                "Failed to derive blind index key for keyRef '" + keyRef + "': " + e.getMessage());
        }
    }

    /**
     * 验证默认密钥的可用性
     */
    private void validateDefaultKey() {
        if (defaultKey == null || defaultKey.isEmpty()) {
            log.warn("No default encryption key configured (afg.data.encryption.default-key). "
                     + "Entities with @EncryptedField(keyRef=\"\") will fail at runtime. "
                     + "Generate a key with: openssl rand -base64 32");
            return;
        }
        try {
            byte[] key = Base64.getDecoder().decode(defaultKey);
            if (key.length != AES_KEY_LENGTH) {
                throw new BusinessException(CommonErrorCode.ENCRYPTION_KEY_INVALID,
                    "Default encryption key must be " + AES_KEY_LENGTH
                    + " bytes (AES-256), but was " + key.length + " bytes");
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_KEY_INVALID,
                "Default encryption key is not valid Base64: " + e.getMessage());
        }
    }
}
