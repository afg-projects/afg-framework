package io.github.afgprojects.framework.integration.redis;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.afgprojects.framework.core.feature.FeatureFlag;
import io.github.afgprojects.framework.core.feature.FeatureFlagManager;
import io.github.afgprojects.framework.integration.redis.feature.RedissonStorageClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedissonStorageClient 集成测试
 *
 * <p>基于真实 Redis 容器测试功能开关的 CRUD 操作
 */
@DisplayName("RedissonStorageClient 功能开关存储测试")
class RedissonStorageClientTest extends BaseRedisTest {

    private RedissonStorageClient storageClient;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        storageClient = new RedissonStorageClient(getRedissonClient(), objectMapper, "test:feature:");
    }

    @Nested
    @DisplayName("put / get 操作")
    class PutGet {

        @Test
        @DisplayName("put 后 get 应返回相同的功能开关")
        void shouldReturnSameFlag_afterPut() {
            FeatureFlag flag = FeatureFlag.of("test-feature", true);
            storageClient.put("test-feature", flag);

            FeatureFlag retrieved = storageClient.get("test-feature");

            assertThat(retrieved).isNotNull();
            assertThat(retrieved.name()).isEqualTo("test-feature");
            assertThat(retrieved.enabled()).isTrue();
        }

        @Test
        @DisplayName("get 不存在的功能开关应返回 null")
        void shouldReturnNull_whenFeatureNotExists() {
            FeatureFlag result = storageClient.get("nonexistent-feature");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("put 应支持 disabled 的功能开关")
        void shouldSupportDisabledFlag() {
            FeatureFlag flag = FeatureFlag.of("disabled-feature", false);
            storageClient.put("disabled-feature", flag);

            FeatureFlag retrieved = storageClient.get("disabled-feature");
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.enabled()).isFalse();
        }

        @Test
        @DisplayName("put 应覆盖已存在的功能开关")
        void shouldOverwriteExistingFlag() {
            storageClient.put("overwrite-feature", FeatureFlag.of("overwrite-feature", true));
            storageClient.put("overwrite-feature", FeatureFlag.of("overwrite-feature", false));

            FeatureFlag retrieved = storageClient.get("overwrite-feature");
            assertThat(retrieved.enabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("putAll 操作")
    class PutAll {

        @Test
        @DisplayName("putAll 应批量存储多个功能开关")
        void shouldStoreMultipleFlags() {
            Map<String, FeatureFlag> flags = Map.of(
                    "feature-a", FeatureFlag.of("feature-a", true),
                    "feature-b", FeatureFlag.of("feature-b", false)
            );

            storageClient.putAll(flags);

            assertThat(storageClient.get("feature-a")).isNotNull();
            assertThat(storageClient.get("feature-a").enabled()).isTrue();
            assertThat(storageClient.get("feature-b")).isNotNull();
            assertThat(storageClient.get("feature-b").enabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("remove 操作")
    class Remove {

        @Test
        @DisplayName("remove 应删除功能开关")
        void shouldRemoveFeature() {
            storageClient.put("remove-feature", FeatureFlag.of("remove-feature", true));
            assertThat(storageClient.get("remove-feature")).isNotNull();

            storageClient.remove("remove-feature");
            assertThat(storageClient.get("remove-feature")).isNull();
        }
    }

    @Nested
    @DisplayName("getAll 操作")
    class GetAll {

        @Test
        @DisplayName("getAll 应返回所有功能开关")
        void shouldReturnAllFlags() {
            storageClient.put("all-a", FeatureFlag.of("all-a", true));
            storageClient.put("all-b", FeatureFlag.of("all-b", false));

            Map<String, FeatureFlag> all = storageClient.getAll();

            assertThat(all).containsKey("all-a");
            assertThat(all).containsKey("all-b");
        }

        @Test
        @DisplayName("getAll 在无数据时应返回空 Map")
        void shouldReturnEmptyMap_whenNoData() {
            // 使用全新的前缀确保没有数据
            RedissonStorageClient freshClient = new RedissonStorageClient(
                    getRedissonClient(), objectMapper, "test:feature:empty:");

            Map<String, FeatureFlag> all = freshClient.getAll();

            assertThat(all).isEmpty();
        }
    }

    @AfterEach
    void cleanup() {
        getRedissonClient().getKeys().getKeysByPattern("test:feature:*").forEach(key -> {
            getRedissonClient().getBucket(key).delete();
        });
        // 清理 RMap
        try {
            getRedissonClient().getMap("test:feature:flags").delete();
            getRedissonClient().getMap("test:feature:empty:flags").delete();
        } catch (Exception ignored) {
            // ignore cleanup errors
        }
    }
}
