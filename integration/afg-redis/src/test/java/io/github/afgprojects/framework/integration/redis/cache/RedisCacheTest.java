package io.github.afgprojects.framework.integration.redis.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * RedisCache 集成测试
 */
@Testcontainers
@DisplayName("RedisCache 集成测试")
class RedisCacheTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private RedissonClient redissonClient;
    private RedisCache<String> stringCache;
    private RedisCache<TestUser> userCache;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

        redissonClient = Redisson.create(config);
        stringCache = new RedisCache<>(redissonClient, "strings", Duration.ofMinutes(5));
        userCache = new RedisCache<>(redissonClient, "users", Duration.ofMinutes(10));
    }

    @AfterEach
    void tearDown() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Nested
    @DisplayName("基本操作测试")
    class BasicOperationsTests {

        @Test
        @DisplayName("应该成功存取字符串值")
        void shouldPutAndGetStringValue() {
            stringCache.put("key1", "value1");

            String result = stringCache.get("key1");

            assertThat(result).isEqualTo("value1");
        }

        @Test
        @DisplayName("应该成功存取对象值")
        void shouldPutAndGetObjectValue() {
            TestUser user = new TestUser(1L, "Alice", "alice@example.com");
            userCache.put("user:1", user);

            TestUser result = userCache.get("user:1");

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("键不存在时应该返回 null")
        void shouldReturnNullWhenKeyNotExists() {
            String result = stringCache.get("non-existent-key");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("应该成功删除缓存值")
        void shouldDeleteValue() {
            stringCache.put("key-to-delete", "value");

            stringCache.evict("key-to-delete");

            assertThat(stringCache.get("key-to-delete")).isNull();
        }

        @Test
        @DisplayName("应该正确检查键是否存在")
        void shouldCheckKeyExists() {
            stringCache.put("existing-key", "value");

            assertThat(stringCache.containsKey("existing-key")).isTrue();
            assertThat(stringCache.containsKey("non-existent-key")).isFalse();
        }
    }

    @Nested
    @DisplayName("TTL 过期测试")
    class TtlTests {

        @Test
        @DisplayName("使用默认 TTL 的值应该自动过期")
        void shouldExpireWithDefaultTtl() {
            RedisCache<String> shortTtlCache = new RedisCache<>(
                    redissonClient, "short-ttl", Duration.ofSeconds(1)
            );

            shortTtlCache.put("expiring-key", "value");

            assertThat(shortTtlCache.get("expiring-key")).isEqualTo("value");

            await().atMost(Duration.ofSeconds(3))
                   .untilAsserted(() -> assertThat(shortTtlCache.get("expiring-key")).isNull());
        }

        @Test
        @DisplayName("使用自定义 TTL 的值应该自动过期")
        void shouldExpireWithCustomTtl() {
            stringCache.put("custom-ttl-key", "value", Duration.ofSeconds(1).toMillis());

            assertThat(stringCache.get("custom-ttl-key")).isEqualTo("value");

            await().atMost(Duration.ofSeconds(3))
                   .untilAsserted(() -> assertThat(stringCache.get("custom-ttl-key")).isNull());
        }

        @Test
        @DisplayName("应该覆盖已存在的值")
        void shouldOverwriteExistingValue() {
            stringCache.put("overwrite-key", "original");
            stringCache.put("overwrite-key", "updated");

            String result = stringCache.get("overwrite-key");

            assertThat(result).isEqualTo("updated");
        }
    }

    @Nested
    @DisplayName("多缓存实例测试")
    class MultipleCacheTests {

        @Test
        @DisplayName("不同名称的缓存应该隔离")
        void shouldBeIsolatedByCacheName() {
            RedisCache<String> cache1 = new RedisCache<>(redissonClient, "cache1", 0);
            RedisCache<String> cache2 = new RedisCache<>(redissonClient, "cache2", 0);

            cache1.put("same-key", "value-from-cache1");
            cache2.put("same-key", "value-from-cache2");

            assertThat(cache1.get("same-key")).isEqualTo("value-from-cache1");
            assertThat(cache2.get("same-key")).isEqualTo("value-from-cache2");
        }
    }

    @Nested
    @DisplayName("缓存名称测试")
    class CacheNameTests {

        @Test
        @DisplayName("应该返回正确的缓存名称")
        void shouldReturnCorrectCacheName() {
            assertThat(stringCache.getName()).isEqualTo("strings");
            assertThat(userCache.getName()).isEqualTo("users");
        }
    }

    @Nested
    @DisplayName("缓存大小测试")
    class CacheSizeTests {

        @Test
        @DisplayName("应该正确返回缓存大小")
        void shouldReturnCorrectCacheSize() {
            stringCache.put("key1", "value1");
            stringCache.put("key2", "value2");
            stringCache.put("key3", "value3");

            assertThat(stringCache.size()).isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("清空缓存测试")
    class ClearCacheTests {

        @Test
        @DisplayName("应该成功清空缓存")
        void shouldClearCache() {
            stringCache.put("key1", "value1");
            stringCache.put("key2", "value2");

            stringCache.clear();

            assertThat(stringCache.get("key1")).isNull();
            assertThat(stringCache.get("key2")).isNull();
        }
    }

    /**
     * 测试用户记录
     */
    record TestUser(Long id, String name, String email) {}
}