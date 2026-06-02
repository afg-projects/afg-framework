package io.github.afgprojects.framework.ai.security;

import io.github.afgprojects.framework.ai.core.api.security.ApiKeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultApiKeyManager 单元测试
 */
class DefaultApiKeyManagerTest {

    private DefaultApiKeyManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultApiKeyManager();
    }

    @Test
    @DisplayName("存储和获取 API Key")
    void storeAndGetKey() {
        ApiKeyManager.ApiKey key = createApiKey("default", "sk-test-key-123", "openai");
        ApiKeyManager.ApiKeyContext context = createContext("user-001", "tenant-001");

        manager.storeKey("openai", key, context);

        ApiKeyManager.ApiKey retrieved = manager.getKey("openai", context);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getValue()).isEqualTo("sk-test-key-123");
        assertThat(retrieved.getName()).isEqualTo("default");
        assertThat(retrieved.getProvider()).isEqualTo("openai");
    }

    @Test
    @DisplayName("获取不存在的 API Key")
    void getKey_notFound() {
        ApiKeyManager.ApiKeyContext context = createContext("user-001", "tenant-001");

        ApiKeyManager.ApiKey key = manager.getKey("anthropic", context);

        assertThat(key).isNull();
    }

    @Test
    @DisplayName("删除 API Key")
    void deleteKey() {
        ApiKeyManager.ApiKey key = createApiKey("default", "sk-test-key-123", "openai");
        ApiKeyManager.ApiKeyContext context = createContext("user-001", "tenant-001");

        manager.storeKey("openai", key, context);
        manager.deleteKey("openai", "default", context);

        ApiKeyManager.ApiKey retrieved = manager.getKey("openai", context);

        assertThat(retrieved).isNull();
    }

    @Test
    @DisplayName("轮换 API Key")
    void rotateKey() {
        ApiKeyManager.ApiKey oldKey = createApiKey("default", "sk-old-key", "openai");
        ApiKeyManager.ApiKey newKey = createApiKey("default", "sk-new-key", "openai");
        ApiKeyManager.ApiKeyContext context = createContext("user-001", "tenant-001");

        manager.storeKey("openai", oldKey, context);
        manager.rotateKey("openai", "default", newKey, context);

        ApiKeyManager.ApiKey retrieved = manager.getKey("openai", context);

        assertThat(retrieved.getValue()).isEqualTo("sk-new-key");
    }

    @Test
    @DisplayName("列出所有 API Key")
    void listKeys() {
        ApiKeyManager.ApiKey key1 = createApiKey("default", "sk-key-1", "openai");
        ApiKeyManager.ApiKey key2 = createApiKey("backup", "sk-key-2", "openai");
        ApiKeyManager.ApiKeyContext context = createContext("user-001", "tenant-001");

        manager.storeKey("openai", key1, context);
        manager.storeKey("openai", key2, context);

        Set<ApiKeyManager.ApiKeyInfo> keys = manager.listKeys("openai", context);

        assertThat(keys).hasSize(2);
    }

    @Test
    @DisplayName("检查权限")
    void hasPermission() {
        Set<String> permissions = new HashSet<>();
        permissions.add("chat");
        permissions.add("completion");

        ApiKeyManager.ApiKey key = createApiKeyWithPermissions("default", "sk-test-key", "openai", permissions);
        ApiKeyManager.ApiKeyContext context = createContext("user-001", "tenant-001");

        manager.storeKey("openai", key, context);

        assertThat(manager.hasPermission("openai", "default", "chat", context)).isTrue();
        assertThat(manager.hasPermission("openai", "default", "completion", context)).isTrue();
        assertThat(manager.hasPermission("openai", "default", "embedding", context)).isFalse();
    }

    @Test
    @DisplayName("通配符权限")
    void hasPermission_wildcard() {
        Set<String> permissions = new HashSet<>();
        permissions.add("*");

        ApiKeyManager.ApiKey key = createApiKeyWithPermissions("default", "sk-test-key", "openai", permissions);
        ApiKeyManager.ApiKeyContext context = createContext("user-001", "tenant-001");

        manager.storeKey("openai", key, context);

        assertThat(manager.hasPermission("openai", "default", "chat", context)).isTrue();
        assertThat(manager.hasPermission("openai", "default", "embedding", context)).isTrue();
    }

    private ApiKeyManager.ApiKey createApiKey(String name, String value, String provider) {
        return createApiKeyWithPermissions(name, value, provider, new HashSet<>());
    }

    private ApiKeyManager.ApiKey createApiKeyWithPermissions(String name, String value, String provider, Set<String> permissions) {
        return new ApiKeyManager.ApiKey() {
            @Override
            public String getName() { return name; }
            @Override
            public String getValue() { return value; }
            @Override
            public String getProvider() { return provider; }
            @Override
            public Set<String> getPermissions() { return permissions; }
            @Override
            public Long getExpiresAt() { return null; }
            @Override
            public java.util.Map<String, String> getMetadata() { return java.util.Map.of(); }
            @Override
            public boolean isExpired() { return false; }
            @Override
            public boolean isEnabled() { return true; }
        };
    }

    private ApiKeyManager.ApiKeyContext createContext(String userId, String tenantId) {
        return new ApiKeyManager.ApiKeyContext() {
            @Override
            public String getUserId() { return userId; }
            @Override
            public String getTenantId() { return tenantId; }
            @Override
            public String getApplicationId() { return null; }
            @Override
            public java.util.Map<String, String> getAttributes() { return java.util.Map.of(); }
        };
    }
}