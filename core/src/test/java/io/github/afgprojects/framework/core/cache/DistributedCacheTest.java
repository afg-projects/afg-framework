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
 * DistributedCache 单元测试
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

    @Nested
    @DisplayName("基本操作测试")
    class BasicOperationTests {

        @Test
        @DisplayName("应该正确获取缓存名称")
        void shouldGetName() {
            assertThat(cache.getName()).isEqualTo("test-cache");
        }

        @Test
        @DisplayName("应该正确获取配置")
        void shouldGetConfig() {
            assertThat(cache.getConfig()).isNotNull();
        }

        @Test
        @DisplayName("应该正确获取 Redisson 客户端")
        void shouldGetRedissonClient() {
            assertThat(cache.getRedissonClient()).isEqualTo(redissonClient);
        }

        @Test
        @DisplayName("应该正确获取指标")
        void shouldGetMetrics() {
            assertThat(cache.getMetrics()).isNotNull();
        }
    }

    @Nested
    @DisplayName("get 操作测试")
    class GetTests {

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

    @Nested
    @DisplayName("put 操作测试")
    class PutTests {

        @Test
        @DisplayName("应该存储值")
        void shouldPutValue() {
            // when
            cache.put("key1", "value1");

            // then
            verify(bucket).set(any());
        }

        @Test
        @DisplayName("应该支持自定义 TTL")
        void shouldPutWithTtl() {
            // when
            cache.put("key1", "value1", 5000);

            // then
            verify(bucket).set(any(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("evict 操作测试")
    class EvictTests {

        @Test
        @DisplayName("应该删除缓存键")
        void shouldEvictKey() {
            // when
            cache.evict("key1");

            // then
            verify(bucket).delete();
        }
    }

    @Nested
    @DisplayName("containsKey 操作测试")
    class ContainsKeyTests {

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

    @Nested
    @DisplayName("putIfAbsent 操作测试")
    class PutIfAbsentTests {

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

    @Nested
    @DisplayName("getOrLoad 操作测试")
    class GetOrLoadTests {

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

    @Nested
    @DisplayName("clear 操作测试")
    class ClearTests {

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

    @Nested
    @DisplayName("size 操作测试")
    class SizeTests {

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

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

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
