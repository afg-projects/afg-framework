package io.github.afgprojects.framework.ai.security;

import io.github.afgprojects.framework.ai.core.api.security.ApiKeyManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 API Key 管理器实现
 *
 * <p>基于内存的简单 API Key 管理器，适用于：
 * <ul>
 *   <li>开发测试环境</li>
 *   <li>单机部署</li>
 *   <li>不需要持久化的场景</li>
 * </ul>
 *
 * <p>生产环境建议使用数据库或专业密钥管理系统（如 Vault、AWS KMS）实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultApiKeyManager implements ApiKeyManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultApiKeyManager.class);

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec encryptionKey;
    private final ConcurrentHashMap<String, StoredApiKey> keys = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<ApiKeyUsage>> usageHistory = new ConcurrentHashMap<>();

    /**
     * 创建默认 API Key 管理器
     *
     * @param encryptionKey 加密密钥（32 字节用于 AES-256）
     */
    public DefaultApiKeyManager(byte[] encryptionKey) {
        if (encryptionKey.length != 16 && encryptionKey.length != 32) {
            throw new IllegalArgumentException("Encryption key must be 16 or 32 bytes");
        }
        this.encryptionKey = new SecretKeySpec(encryptionKey, "AES");
    }

    /**
     * 创建默认 API Key 管理器（使用随机密钥）
     */
    public DefaultApiKeyManager() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        this.encryptionKey = new SecretKeySpec(key, "AES");
    }

    @Override
    @Nullable
    public ApiKey getKey(@NonNull String provider, @NonNull ApiKeyContext context) {
        return getKey(provider, "default", context);
    }

    @Override
    @Nullable
    public ApiKey getKey(@NonNull String provider, @NonNull String keyName, @NonNull ApiKeyContext context) {
        String key = buildKey(provider, keyName, context);
        StoredApiKey stored = keys.get(key);

        if (stored == null) {
            return null;
        }

        if (stored.isExpired()) {
            log.warn("API key {} for provider {} is expired", keyName, provider);
            return null;
        }

        if (!stored.isEnabled()) {
            log.warn("API key {} for provider {} is disabled", keyName, provider);
            return null;
        }

        // 解密密钥值
        String decryptedValue = decrypt(stored.getEncryptedValue());

        return new DefaultApiKey(keyName, decryptedValue, provider, stored.getPermissions(),
                stored.getExpiresAt(), stored.getMetadata(), stored.isEnabled());
    }

    @Override
    public void storeKey(@NonNull String provider, @NonNull ApiKey key, @NonNull ApiKeyContext context) {
        String keyId = buildKey(provider, key.getName(), context);
        String encryptedValue = encrypt(key.getValue());

        StoredApiKey stored = new StoredApiKey(
                key.getName(),
                encryptedValue,
                provider,
                key.getPermissions(),
                key.getExpiresAt(),
                key.getMetadata(),
                true,
                System.currentTimeMillis()
        );

        keys.put(keyId, stored);

        log.info("Stored API key {} for provider {}", key.getName(), provider);
    }

    @Override
    public void rotateKey(@NonNull String provider, @NonNull String keyName, @NonNull ApiKey newKey, @NonNull ApiKeyContext context) {
        storeKey(provider, newKey, context);

        log.info("Rotated API key {} for provider {}", keyName, provider);
    }

    @Override
    public void deleteKey(@NonNull String provider, @NonNull String keyName, @NonNull ApiKeyContext context) {
        String keyId = buildKey(provider, keyName, context);
        keys.remove(keyId);
        usageHistory.remove(keyId);

        log.info("Deleted API key {} for provider {}", keyName, provider);
    }

    @Override
    @NonNull
    public Set<ApiKeyInfo> listKeys(@NonNull String provider, @NonNull ApiKeyContext context) {
        Set<ApiKeyInfo> result = new HashSet<>();

        keys.forEach((key, stored) -> {
            if (stored.getProvider().equals(provider)) {
                result.add(new DefaultApiKeyInfo(
                        stored.getName(),
                        stored.getProvider(),
                        stored.getPermissions(),
                        stored.getCreatedAt(),
                        stored.getExpiresAt(),
                        stored.isEnabled(),
                        getLastUsedAt(key)
                ));
            }
        });

        return result;
    }

    @Override
    public boolean hasPermission(@NonNull String provider, @NonNull String keyName, @NonNull String operation, @NonNull ApiKeyContext context) {
        String keyId = buildKey(provider, keyName, context);
        StoredApiKey stored = keys.get(keyId);

        if (stored == null || !stored.isEnabled() || stored.isExpired()) {
            return false;
        }

        return stored.getPermissions().contains(operation) || stored.getPermissions().contains("*");
    }

    @Override
    public void recordUsage(@NonNull String provider, @NonNull String keyName, @NonNull ApiKeyUsage usage) {
        String keyId = provider + ":" + keyName;
        usageHistory.computeIfAbsent(keyId, k -> new ArrayList<>()).add(usage);
    }

    private String buildKey(String provider, String keyName, ApiKeyContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(provider).append(":").append(keyName);

        if (context.getTenantId() != null) {
            sb.append(":tenant:").append(context.getTenantId());
        }

        return sb.toString();
    }

    @Nullable
    private Long getLastUsedAt(String keyId) {
        List<ApiKeyUsage> history = usageHistory.get(keyId);
        if (history == null || history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1).getTimestamp();
    }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 组合 IV 和加密数据
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt API key", e);
        }
    }

    private String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt API key", e);
        }
    }

    /**
     * 存储的 API Key
     */
    private static class StoredApiKey {
        private final String name;
        private final String encryptedValue;
        private final String provider;
        private final Set<String> permissions;
        private final Long expiresAt;
        private final Map<String, String> metadata;
        private final boolean enabled;
        private final long createdAt;

        StoredApiKey(String name, String encryptedValue, String provider, Set<String> permissions,
                     Long expiresAt, Map<String, String> metadata, boolean enabled, long createdAt) {
            this.name = name;
            this.encryptedValue = encryptedValue;
            this.provider = provider;
            this.permissions = permissions;
            this.expiresAt = expiresAt;
            this.metadata = metadata;
            this.enabled = enabled;
            this.createdAt = createdAt;
        }

        String getName() { return name; }
        String getEncryptedValue() { return encryptedValue; }
        String getProvider() { return provider; }
        Set<String> getPermissions() { return permissions; }
        Long getExpiresAt() { return expiresAt; }
        Map<String, String> getMetadata() { return metadata; }
        boolean isEnabled() { return enabled; }
        long getCreatedAt() { return createdAt; }

        boolean isExpired() {
            return expiresAt != null && System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * 默认 API Key 实现
     */
    private static class DefaultApiKey implements ApiKey {
        private final String name;
        private final String value;
        private final String provider;
        private final Set<String> permissions;
        private final Long expiresAt;
        private final Map<String, String> metadata;
        private final boolean enabled;

        DefaultApiKey(String name, String value, String provider, Set<String> permissions,
                      Long expiresAt, Map<String, String> metadata, boolean enabled) {
            this.name = name;
            this.value = value;
            this.provider = provider;
            this.permissions = permissions;
            this.expiresAt = expiresAt;
            this.metadata = metadata;
            this.enabled = enabled;
        }

        @Override
        @NonNull
        public String getName() { return name; }

        @Override
        @NonNull
        public String getValue() { return value; }

        @Override
        @NonNull
        public String getProvider() { return provider; }

        @Override
        @NonNull
        public Set<String> getPermissions() { return permissions; }

        @Override
        @Nullable
        public Long getExpiresAt() { return expiresAt; }

        @Override
        @NonNull
        public Map<String, String> getMetadata() { return metadata; }

        @Override
        public boolean isExpired() {
            return expiresAt != null && System.currentTimeMillis() > expiresAt;
        }

        @Override
        public boolean isEnabled() { return enabled; }
    }

    /**
     * 默认 API Key 信息实现
     */
    private static class DefaultApiKeyInfo implements ApiKeyInfo {
        private final String name;
        private final String provider;
        private final Set<String> permissions;
        private final long createdAt;
        private final Long expiresAt;
        private final boolean enabled;
        private final Long lastUsedAt;

        DefaultApiKeyInfo(String name, String provider, Set<String> permissions, long createdAt,
                          Long expiresAt, boolean enabled, Long lastUsedAt) {
            this.name = name;
            this.provider = provider;
            this.permissions = permissions;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.enabled = enabled;
            this.lastUsedAt = lastUsedAt;
        }

        @Override
        @NonNull
        public String getName() { return name; }

        @Override
        @NonNull
        public String getProvider() { return provider; }

        @Override
        @NonNull
        public Set<String> getPermissions() { return permissions; }

        @Override
        public long getCreatedAt() { return createdAt; }

        @Override
        @Nullable
        public Long getExpiresAt() { return expiresAt; }

        @Override
        public boolean isEnabled() { return enabled; }

        @Override
        @Nullable
        public Long getLastUsedAt() { return lastUsedAt; }
    }
}