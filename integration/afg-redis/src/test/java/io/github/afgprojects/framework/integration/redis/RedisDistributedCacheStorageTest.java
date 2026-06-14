package io.github.afgprojects.framework.integration.redis;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cache.spi.DistributedCacheStorage;
import io.github.afgprojects.framework.integration.redis.cache.RedisDistributedCacheStorage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisDistributedCacheStorage 集成测试
 *
 * <p>基于真实 Redis 容器测试分布式缓存存储操作
 */
@DisplayName("RedisDistributedCacheStorage 分布式缓存测试")
class RedisDistributedCacheStorageTest extends BaseRedisTest {

    private static final String KEY_PREFIX = "test:cache:";

    private DistributedCacheStorage cacheStorage;

    @BeforeEach
    void setUp() {
        cacheStorage = new RedisDistributedCacheStorage(getRedissonClient(), KEY_PREFIX);
    }

    @Nested
    @DisplayName("getStorageType")
    class StorageType {

        @Test
        @DisplayName("getStorageType 应返回 redis")
        void shouldReturnRedis() {
            assertThat(cacheStorage.getStorageType()).isEqualTo("redis");
        }
    }

    @Nested
    @DisplayName("get / set 操作")
    class GetSet {

        @Test
        @DisplayName("set 后 get 应返回相同值")
        void shouldReturnSameValue_afterSet() {
            cacheStorage.set("key1", "value1");

            Object result = cacheStorage.get("key1");
            assertThat(result).isEqualTo("value1");
        }

        @Test
        @DisplayName("get 不存在的 key 应返回 null")
        void shouldReturnNull_whenKeyNotExists() {
            Object result = cacheStorage.get("nonexistent");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("set 应支持各种类型的值")
        void shouldSupportVariousValueTypes() {
            cacheStorage.set("int-key", 42);
            cacheStorage.set("long-key", 9999999999L);
            cacheStorage.set("bool-key", true);
            cacheStorage.set("double-key", 3.14);

            assertThat(cacheStorage.get("int-key")).isEqualTo(42);
            assertThat(cacheStorage.get("long-key")).isEqualTo(9999999999L);
            assertThat(cacheStorage.get("bool-key")).isEqualTo(true);
            assertThat(cacheStorage.get("double-key")).isEqualTo(3.14);
        }
    }

    @Nested
    @DisplayName("set 带 TTL 操作")
    class SetWithTtl {

        @Test
        @DisplayName("set 带 TTL 应正常存储值")
        void shouldStoreValue_withTtl() {
            cacheStorage.set("ttl-key", "ttl-value", Duration.ofSeconds(60));

            assertThat(cacheStorage.get("ttl-key")).isEqualTo("ttl-value");
            assertThat(cacheStorage.exists("ttl-key")).isTrue();
        }
    }

    @Nested
    @DisplayName("setIfAbsent 操作")
    class SetIfAbsent {

        @Test
        @DisplayName("setIfAbsent 对不存在的 key 应成功")
        void shouldSucceed_whenKeyNotExists() {
            boolean result = cacheStorage.setIfAbsent("absent-key", "initial-value", Duration.ofSeconds(60));

            assertThat(result).isTrue();
            assertThat(cacheStorage.get("absent-key")).isEqualTo("initial-value");
        }

        @Test
        @DisplayName("setIfAbsent 对已存在的 key 应失败")
        void shouldFail_whenKeyExists() {
            cacheStorage.set("existing-key", "existing-value");

            boolean result = cacheStorage.setIfAbsent("existing-key", "new-value", Duration.ofSeconds(60));

            assertThat(result).isFalse();
            assertThat(cacheStorage.get("existing-key")).isEqualTo("existing-value");
        }
    }

    @Nested
    @DisplayName("delete 操作")
    class Delete {

        @Test
        @DisplayName("delete 应删除存在的 key")
        void shouldDeleteExistingKey() {
            cacheStorage.set("delete-key", "delete-value");
            assertThat(cacheStorage.exists("delete-key")).isTrue();

            cacheStorage.delete("delete-key");

            assertThat(cacheStorage.exists("delete-key")).isFalse();
            assertThat(cacheStorage.get("delete-key")).isNull();
        }
    }

    @Nested
    @DisplayName("exists 操作")
    class Exists {

        @Test
        @DisplayName("exists 应正确判断 key 是否存在")
        void shouldDetectKeyExistence() {
            assertThat(cacheStorage.exists("check-key")).isFalse();

            cacheStorage.set("check-key", "check-value");
            assertThat(cacheStorage.exists("check-key")).isTrue();

            cacheStorage.delete("check-key");
            assertThat(cacheStorage.exists("check-key")).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteByPattern 操作")
    class DeleteByPattern {

        @Test
        @DisplayName("deleteByPattern 应删除匹配模式的所有 key")
        void shouldDeleteMatchingKeys() {
            cacheStorage.set("pattern:a", "value-a");
            cacheStorage.set("pattern:b", "value-b");
            cacheStorage.set("pattern:c", "value-c");
            cacheStorage.set("other:x", "value-x");

            cacheStorage.deleteByPattern(KEY_PREFIX + "pattern:*");

            assertThat(cacheStorage.exists("pattern:a")).isFalse();
            assertThat(cacheStorage.exists("pattern:b")).isFalse();
            assertThat(cacheStorage.exists("pattern:c")).isFalse();
            assertThat(cacheStorage.exists("other:x")).isTrue();
        }
    }

    @Nested
    @DisplayName("countByPattern 操作")
    class CountByPattern {

        @Test
        @DisplayName("countByPattern 应返回匹配模式的 key 数量")
        void shouldCountMatchingKeys() {
            cacheStorage.set("count:a", "value-a");
            cacheStorage.set("count:b", "value-b");
            cacheStorage.set("count:c", "value-c");

            long count = cacheStorage.countByPattern(KEY_PREFIX + "count:*");
            assertThat(count).isEqualTo(3);
        }
    }

    @AfterEach
    void cleanup() {
        // 清理所有测试 key
        getRedissonClient().getKeys().getKeysByPattern(KEY_PREFIX + "*").forEach(key -> {
            getRedissonClient().getBucket(key).delete();
        });
    }
}
