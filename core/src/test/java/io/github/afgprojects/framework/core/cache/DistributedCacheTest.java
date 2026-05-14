package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * DistributedCache 单元测试。
 * <p>
 * 测试分布式缓存的基本操作，包括 get、put、evict、containsKey、putIfAbsent、getOrLoad 等方法。
 * </p>
 *
 * @see DistributedCache
 */
@DisplayName("DistributedCache 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistributedCacheTest extends BaseUnitTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<Object> bucket;

    @Mock
    private org.redisson.api.RKeys rKeys;

    private DistributedCache<String> cache;
    private CacheConfig config;

    @BeforeEach
    void setUp() {
        config = CacheConfig.defaultConfig();
        lenient().when(redissonClient.getBucket(anyString())).thenReturn(bucket);
        lenient().when(redissonClient.getKeys()).thenReturn(rKeys);
        lenient().when(rKeys.getKeysByPattern(anyString())).thenReturn(Collections.emptyList());
        cache = new DistributedCache<>("test-cache", config, redissonClient);
    }

    /**
     * 基本操作测试。
     * <p>
     * 测试缓存名称获取、配置获取、Redisson 客户端获取和指标获取等基本功能。
     * </p>
     */
    @Nested
    @DisplayName("基本操作测试")
    class BasicOperationTests {

        /**
         * 测试获取缓存名称。
         */
        @Test
        @DisplayName("应该正确获取缓存名称")
        void shouldGetName() {
            assertThat(cache.getName()).isEqualTo("test-cache");
        }

        /**
         * 测试获取缓存配置。
         */
        @Test
        @DisplayName("应该正确获取配置")
        void shouldGetConfig() {
            assertThat(cache.getConfig()).isNotNull();
        }

        /**
         * 测试获取 Redisson 客户端。
         */
        @Test
        @DisplayName("应该正确获取 Redisson 客户端")
        void shouldGetRedissonClient() {
            assertThat(cache.getRedissonClient()).isEqualTo(redissonClient);
        }

        /**
         * 测试获取缓存指标。
         */
        @Test
        @DisplayName("应该正确获取指标")
        void shouldGetMetrics() {
            assertThat(cache.getMetrics()).isNotNull();
        }
    }

    /**
     * get 操作测试。
     * <p>
     * 测试缓存值的获取，包括正常获取、缓存不存在和 NullValue 处理等场景。
     * </p>
     */
    @Nested
    @DisplayName("get 操作测试")
    class GetTests {

        /**
         * 测试正常获取缓存值。
         */
        @Test
        @DisplayName("应该获取缓存值")
        void shouldGetValue() {
            // given
            when(bucket.get()).thenReturn("test-value");

            // when
            String value = cache.get("key1");

            // then
            assertThat(value).isEqualTo("test-value");
        }

        /**
         * 测试缓存不存在时返回 null。
         */
        @Test
        @DisplayName("缓存不存在时应该返回 null")
        void shouldReturnNullWhenNotExists() {
            // given
            when(bucket.get()).thenReturn(null);

            // when
            String value = cache.get("key1");

            // then
            assertThat(value).isNull();
        }

        /**
         * 测试正确处理 NullValue 占位符。
         */
        @Test
        @DisplayName("应该正确处理 NullValue")
        void shouldHandleNullValue() {
            // given
            when(bucket.get()).thenReturn(NullValue.INSTANCE);

            // when
            String value = cache.get("key1");

            // then
            assertThat(value).isNull();
        }
    }

    /**
     * put 操作测试。
     * <p>
     * 测试缓存值的存储，包括基本存储和自定义 TTL 存储。
     * </p>
     */
    @Nested
    @DisplayName("put 操作测试")
    class PutTests {

        /**
         * 测试存储缓存值。
         */
        @Test
        @DisplayName("应该存储值")
        void shouldPutValue() {
            // when
            cache.put("key1", "value1");

            // then
            verify(bucket).set(any());
        }

        /**
         * 测试使用自定义 TTL 存储缓存值。
         */
        @Test
        @DisplayName("应该支持自定义 TTL")
        void shouldPutWithTtl() {
            // when
            cache.put("key1", "value1", 5000);

            // then
            verify(bucket).set(any(), any(Duration.class));
        }
    }

    /**
     * evict 操作测试。
     * <p>
     * 测试缓存键的删除操作。
     * </p>
     */
    @Nested
    @DisplayName("evict 操作测试")
    class EvictTests {

        /**
         * 测试删除缓存键。
         */
        @Test
        @DisplayName("应该删除缓存键")
        void shouldEvictKey() {
            // when
            cache.evict("key1");

            // then
            verify(bucket).delete();
        }
    }

    /**
     * containsKey 操作测试。
     * <p>
     * 测试缓存键的存在性检查。
     * </p>
     */
    @Nested
    @DisplayName("containsKey 操作测试")
    class ContainsKeyTests {

        /**
         * 测试键存在时返回 true。
         */
        @Test
        @DisplayName("应该检查键是否存在")
        void shouldCheckKeyExists() {
            // given
            when(bucket.isExists()).thenReturn(true);

            // when
            boolean exists = cache.containsKey("key1");

            // then
            assertThat(exists).isTrue();
        }

        /**
         * 测试键不存在时返回 false。
         */
        @Test
        @DisplayName("键不存在时应该返回 false")
        void shouldReturnFalseWhenKeyNotExists() {
            // given
            when(bucket.isExists()).thenReturn(false);

            // when
            boolean exists = cache.containsKey("key1");

            // then
            assertThat(exists).isFalse();
        }
    }

    /**
     * putIfAbsent 操作测试。
     * <p>
     * 测试仅在键不存在时设置值的操作。
     * </p>
     */
    @Nested
    @DisplayName("putIfAbsent 操作测试")
    class PutIfAbsentTests {

        /**
         * 测试成功设置不存在的键。
         */
        @Test
        @DisplayName("应该成功设置不存在的键")
        void shouldPutIfAbsent() {
            // given
            when(bucket.setIfAbsent(any(), any(Duration.class))).thenReturn(true);

            // when
            String result = cache.putIfAbsent("key1", "value1", 5000);

            // then
            assertThat(result).isNull();
        }

        /**
         * 测试键已存在时返回已存在的值。
         */
        @Test
        @DisplayName("键已存在时应该返回已存在的值")
        void shouldReturnExistingWhenKeyExists() {
            // given
            when(bucket.setIfAbsent(any(), any(Duration.class))).thenReturn(false);
            when(bucket.get()).thenReturn("existing-value");

            // when
            String result = cache.putIfAbsent("key1", "value1", 5000);

            // then
            assertThat(result).isEqualTo("existing-value");
        }
    }

    /**
     * getOrLoad 操作测试。
     * <p>
     * 测试缓存未命中时自动加载数据的功能。
     * </p>
     */
    @Nested
    @DisplayName("getOrLoad 操作测试")
    class GetOrLoadTests {

        /**
         * 测试缓存命中时直接返回缓存值。
         */
        @Test
        @DisplayName("缓存命中时应该返回缓存值")
        void shouldReturnCachedValueWhenHit() {
            // given
            when(bucket.get()).thenReturn("cached-value");

            // when
            String value = cache.getOrLoad("key1", () -> "loaded-value");

            // then
            assertThat(value).isEqualTo("cached-value");
        }

        /**
         * 测试缓存未命中时加载并缓存数据。
         */
        @Test
        @DisplayName("缓存未命中时应该加载并缓存")
        void shouldLoadAndCacheWhenMiss() {
            // given
            when(bucket.get()).thenReturn(null);

            // when
            String value = cache.getOrLoad("key1", () -> "loaded-value");

            // then
            assertThat(value).isEqualTo("loaded-value");
            verify(bucket).set(any());
        }
    }

    /**
     * clear 操作测试。
     * <p>
     * 测试清空缓存的功能。
     * </p>
     */
    @Nested
    @DisplayName("clear 操作测试")
    class ClearTests {

        /**
         * 测试清空缓存。
         */
        @Test
        @DisplayName("应该清空缓存")
        void shouldClearCache() {
            // given
            when(redissonClient.getKeys().getKeysByPattern(anyString())).thenReturn(Collections.emptyList());

            // when
            cache.clear();

            // then
            verify(redissonClient.getKeys()).getKeysByPattern(anyString());
        }
    }

    /**
     * size 操作测试。
     * <p>
     * 测试获取缓存大小。
     * </p>
     */
    @Nested
    @DisplayName("size 操作测试")
    class SizeTests {

        /**
         * 测试返回缓存大小。
         */
        @Test
        @DisplayName("应该返回缓存大小")
        void shouldReturnSize() {
            // given
            when(redissonClient.getKeys().getKeysByPattern(anyString())).thenReturn(Collections.emptyList());

            // when
            long size = cache.size();

            // then
            assertThat(size).isGreaterThanOrEqualTo(0);
        }
    }

    /**
     * 静态工厂方法测试。
     * <p>
     * 测试 createDefault 静态工厂方法。
     * </p>
     */
    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        /**
         * 测试创建默认配置的缓存。
         */
        @Test
        @DisplayName("应该创建默认配置的缓存")
        void shouldCreateDefaultCache() {
            // when
            DistributedCache<String> defaultCache = DistributedCache.createDefault("default-cache", redissonClient);

            // then
            assertThat(defaultCache).isNotNull();
            assertThat(defaultCache.getName()).isEqualTo("default-cache");
        }
    }
}
