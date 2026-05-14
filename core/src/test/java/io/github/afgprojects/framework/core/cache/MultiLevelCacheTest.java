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

import io.github.afgprojects.framework.core.cache.spi.DistributedCacheStorage;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * MultiLevelCache 单元测试。
 * <p>
 * 测试多级缓存的功能，包括本地缓存和分布式缓存的协同操作。
 * </p>
 *
 * @see MultiLevelCache
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
    private DistributedCacheStorage storage;

    private MultiLevelCache<String> cache;

    @BeforeEach
    void setUp() {
        cache = new MultiLevelCache<>("test-cache", localCache, distributedCache);
    }

    /**
     * 基本操作测试。
     * <p>
     * 测试缓存名称、本地缓存、分布式缓存和指标的获取。
     * </p>
     */
    @Nested
    @DisplayName("基本操作测试")
    class BasicOperationTests {

        /**
         * 测试正确获取缓存名称。
         */
        @Test
        @DisplayName("应该正确获取缓存名称")
        void shouldGetName() {
            assertThat(cache.getName()).isEqualTo("test-cache");
        }

        /**
         * 测试正确获取本地缓存。
         */
        @Test
        @DisplayName("应该正确获取本地缓存")
        void shouldGetLocalCache() {
            assertThat(cache.getLocalCache()).isEqualTo(localCache);
        }

        /**
         * 测试正确获取分布式缓存。
         */
        @Test
        @DisplayName("应该正确获取分布式缓存")
        void shouldGetDistributedCache() {
            assertThat(cache.getDistributedCache()).isEqualTo(distributedCache);
        }

        /**
         * 测试正确获取指标。
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
     * 测试多级缓存的获取操作，包括本地命中、分布式命中回填和两级未命中。
     * </p>
     */
    @Nested
    @DisplayName("get 操作测试")
    class GetTests {

        /**
         * 测试本地缓存命中时直接返回。
         */
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

        /**
         * 测试本地未命中分布式命中时回填本地缓存。
         */
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

        /**
         * 测试两级缓存都未命中时返回 null。
         */
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

    /**
     * put 操作测试。
     * <p>
     * 测试多级缓存的存储操作，包括同时写入两级缓存和自定义 TTL。
     * </p>
     */
    @Nested
    @DisplayName("put 操作测试")
    class PutTests {

        /**
         * 测试同时写入两级缓存。
         */
        @Test
        @DisplayName("应该同时写入两级缓存")
        void shouldPutBothCaches() {
            // when
            cache.put("key1", "value1");

            // then
            verify(distributedCache).put("key1", "value1", 0);
            verify(localCache).put("key1", "value1", 0);
        }

        /**
         * 测试支持自定义 TTL。
         */
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

    /**
     * evict 操作测试。
     * <p>
     * 测试多级缓存的删除操作，包括清除两级缓存和仅清除本地缓存。
     * </p>
     */
    @Nested
    @DisplayName("evict 操作测试")
    class EvictTests {

        /**
         * 测试清除两级缓存。
         */
        @Test
        @DisplayName("应该清除两级缓存")
        void shouldEvictBothCaches() {
            // when
            cache.evict("key1");

            // then
            verify(localCache).evict("key1");
            verify(distributedCache).evict("key1");
        }

        /**
         * 测试仅清除本地缓存。
         */
        @Test
        @DisplayName("应该仅清除本地缓存")
        void shouldEvictLocalOnly() {
            // when
            cache.evictLocal("key1");

            // then
            verify(localCache).evict("key1");
        }
    }

    /**
     * clear 操作测试。
     * <p>
     * 测试多级缓存的清空操作，包括清空两级缓存和仅清空本地缓存。
     * </p>
     */
    @Nested
    @DisplayName("clear 操作测试")
    class ClearTests {

        /**
         * 测试清空两级缓存。
         */
        @Test
        @DisplayName("应该清空两级缓存")
        void shouldClearBothCaches() {
            // when
            cache.clear();

            // then
            verify(localCache).clear();
            verify(distributedCache).clear();
        }

        /**
         * 测试仅清空本地缓存。
         */
        @Test
        @DisplayName("应该仅清空本地缓存")
        void shouldClearLocalOnly() {
            // when
            cache.clearLocal();

            // then
            verify(localCache).clear();
        }
    }

    /**
     * containsKey 操作测试。
     * <p>
     * 测试多级缓存的键存在性检查。
     * </p>
     */
    @Nested
    @DisplayName("containsKey 操作测试")
    class ContainsKeyTests {

        /**
         * 测试本地缓存存在时返回 true。
         */
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

        /**
         * 测试分布式缓存存在时返回 true。
         */
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

        /**
         * 测试两级缓存都不存在时返回 false。
         */
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

    /**
     * size 操作测试。
     * <p>
     * 测试获取本地缓存大小。
     * </p>
     */
    @Nested
    @DisplayName("size 操作测试")
    class SizeTests {

        /**
         * 测试返回本地缓存大小。
         */
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
            when(distributedCache.putIfAbsent("key1", "value1", 5000)).thenReturn(null);

            // when
            String result = cache.putIfAbsent("key1", "value1", 5000);

            // then
            assertThat(result).isNull();
            verify(localCache).put("key1", "value1", 5000);
        }

        /**
         * 测试键已存在时返回已存在的值。
         */
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
         * 测试本地缓存命中时直接返回。
         */
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

        /**
         * 测试分布式缓存命中时回填本地缓存。
         */
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

        /**
         * 测试两级缓存都未命中时加载数据。
         */
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

    /**
     * 静态工厂方法测试。
     * <p>
     * 测试 create 静态工厂方法创建多级缓存。
     * </p>
     */
    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        /**
         * 测试创建多级缓存。
         */
        @Test
        @DisplayName("应该创建多级缓存")
        void shouldCreateMultiLevelCache() {
            // when
            MultiLevelCache<String> createdCache = MultiLevelCache.create(
                    "new-cache",
                    CacheConfig.defaultConfig(),
                    storage
            );

            // then
            assertThat(createdCache).isNotNull();
            assertThat(createdCache.getName()).isEqualTo("new-cache");
            assertThat(createdCache.getLocalCache()).isNotNull();
            assertThat(createdCache.getDistributedCache()).isNotNull();
        }
    }
}
