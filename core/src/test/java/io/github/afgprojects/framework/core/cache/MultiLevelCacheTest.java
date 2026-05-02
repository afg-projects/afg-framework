package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * MultiLevelCache 单元测试
 */
@DisplayName("MultiLevelCache 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MultiLevelCacheTest extends BaseUnitTest {

    @Mock
    private LocalCache<String> localCache;

    @Mock
    private DistributedCache<String> distributedCache;

    @Mock
    private RedissonClient redissonClient;

    private MultiLevelCache<String> cache;

    @BeforeEach
    void setUp() {
        cache = new MultiLevelCache<>("test-cache", localCache, distributedCache);
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
        @DisplayName("应该正确获取本地缓存")
        void shouldGetLocalCache() {
            assertThat(cache.getLocalCache()).isEqualTo(localCache);
        }

        @Test
        @DisplayName("应该正确获取分布式缓存")
        void shouldGetDistributedCache() {
            assertThat(cache.getDistributedCache()).isEqualTo(distributedCache);
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
        @DisplayName("本地缓存命中时应该直接返回")
        void shouldReturnFromLocalCacheWhenHit() {
            // given
            when(localCache.get("key1")).thenReturn("local-value");

            // when
            String value = cache.get("key1");

            // then
            assertThat(value).isEqualTo("local-value");
            verify(localCache).get("key1");
        }

        @Test
        @DisplayName("本地未命中分布式命中时应该回填本地缓存")
        void shouldBackfillLocalCacheWhenDistributedHit() {
            // given
            when(localCache.get("key1")).thenReturn(null);
            when(distributedCache.get("key1")).thenReturn("distributed-value");

            // when
            String value = cache.get("key1");

            // then
            assertThat(value).isEqualTo("distributed-value");
            verify(localCache).put("key1", "distributed-value");
        }

        @Test
        @DisplayName("两级缓存都未命中时应该返回 null")
        void shouldReturnNullWhenBothMiss() {
            // given
            when(localCache.get("key1")).thenReturn(null);
            when(distributedCache.get("key1")).thenReturn(null);

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
        @DisplayName("应该同时写入两级缓存")
        void shouldPutBothCaches() {
            // when
            cache.put("key1", "value1");

            // then
            verify(distributedCache).put("key1", "value1", 0);
            verify(localCache).put("key1", "value1", 0);
        }

        @Test
        @DisplayName("应该支持自定义 TTL")
        void shouldPutWithTtl() {
            // when
            cache.put("key1", "value1", 5000);

            // then
            verify(distributedCache).put("key1", "value1", 5000);
            verify(localCache).put("key1", "value1", 5000);
        }
    }

    @Nested
    @DisplayName("evict 操作测试")
    class EvictTests {

        @Test
        @DisplayName("应该清除两级缓存")
        void shouldEvictBothCaches() {
            // when
            cache.evict("key1");

            // then
            verify(localCache).evict("key1");
            verify(distributedCache).evict("key1");
        }

        @Test
        @DisplayName("应该仅清除本地缓存")
        void shouldEvictLocalOnly() {
            // when
            cache.evictLocal("key1");

            // then
            verify(localCache).evict("key1");
        }
    }

    @Nested
    @DisplayName("clear 操作测试")
    class ClearTests {

        @Test
        @DisplayName("应该清空两级缓存")
        void shouldClearBothCaches() {
            // when
            cache.clear();

            // then
            verify(localCache).clear();
            verify(distributedCache).clear();
        }

        @Test
        @DisplayName("应该仅清空本地缓存")
        void shouldClearLocalOnly() {
            // when
            cache.clearLocal();

            // then
            verify(localCache).clear();
        }
    }

    @Nested
    @DisplayName("containsKey 操作测试")
    class ContainsKeyTests {

        @Test
        @DisplayName("本地缓存存在时应该返回 true")
        void shouldReturnTrueWhenLocalExists() {
            // given
            when(localCache.containsKey("key1")).thenReturn(true);

            // when
            boolean exists = cache.containsKey("key1");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("分布式缓存存在时应该返回 true")
        void shouldReturnTrueWhenDistributedExists() {
            // given
            when(localCache.containsKey("key1")).thenReturn(false);
            when(distributedCache.containsKey("key1")).thenReturn(true);

            // when
            boolean exists = cache.containsKey("key1");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("两级缓存都不存在时应该返回 false")
        void shouldReturnFalseWhenBothNotExist() {
            // given
            when(localCache.containsKey("key1")).thenReturn(false);
            when(distributedCache.containsKey("key1")).thenReturn(false);

            // when
            boolean exists = cache.containsKey("key1");

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("size 操作测试")
    class SizeTests {

        @Test
        @DisplayName("应该返回本地缓存大小")
        void shouldReturnLocalCacheSize() {
            // given
            when(localCache.size()).thenReturn(10L);

            // when
            long size = cache.size();

            // then
            assertThat(size).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("putIfAbsent 操作测试")
    class PutIfAbsentTests {

        @Test
        @DisplayName("应该成功设置不存在的键")
        void shouldPutIfAbsent() {
            // given
            when(distributedCache.putIfAbsent("key1", "value1", 5000)).thenReturn(null);

            // when
            String result = cache.putIfAbsent("key1", "value1", 5000);

            // then
            assertThat(result).isNull();
            verify(localCache).put("key1", "value1", 5000);
        }

        @Test
        @DisplayName("键已存在时应该返回已存在的值")
        void shouldReturnExistingWhenKeyExists() {
            // given
            when(distributedCache.putIfAbsent("key1", "value1", 5000)).thenReturn("existing-value");

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
        @DisplayName("本地缓存命中时应该直接返回")
        void shouldReturnFromLocalWhenHit() {
            // given
            when(localCache.get("key1")).thenReturn("local-value");

            // when
            String value = cache.getOrLoad("key1", () -> "loaded-value");

            // then
            assertThat(value).isEqualTo("local-value");
        }

        @Test
        @DisplayName("分布式缓存命中时应该回填本地缓存")
        void shouldBackfillLocalWhenDistributedHit() {
            // given
            when(localCache.get("key1")).thenReturn(null);
            when(distributedCache.get("key1")).thenReturn("distributed-value");

            // when
            String value = cache.getOrLoad("key1", () -> "loaded-value");

            // then
            assertThat(value).isEqualTo("distributed-value");
            verify(localCache).put(anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("两级缓存都未命中时应该加载数据")
        void shouldLoadWhenBothMiss() {
            // given
            when(localCache.get("key1")).thenReturn(null);
            when(distributedCache.get("key1")).thenReturn(null);

            // when
            String value = cache.getOrLoad("key1", () -> "loaded-value");

            // then
            assertThat(value).isEqualTo("loaded-value");
            verify(distributedCache).put(anyString(), any(), anyLong());
            verify(localCache).put(anyString(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建多级缓存")
        void shouldCreateMultiLevelCache() {
            // when
            MultiLevelCache<String> createdCache = MultiLevelCache.create(
                    "new-cache",
                    CacheConfig.defaultConfig(),
                    redissonClient
            );

            // then
            assertThat(createdCache).isNotNull();
            assertThat(createdCache.getName()).isEqualTo("new-cache");
            assertThat(createdCache.getLocalCache()).isNotNull();
            assertThat(createdCache.getDistributedCache()).isNotNull();
        }
    }
}
