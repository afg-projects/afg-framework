package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cache.metrics.CacheMetrics;

/**
 * LocalCache 单元测试。
 * <p>
 * 测试本地缓存的基本操作，包括存取、删除、清空、Null 值处理、putIfAbsent、getOrLoad、缓存指标和缓存大小等功能。
 * </p>
 *
 * @see LocalCache
 */
@DisplayName("LocalCache 测试")
class LocalCacheTest {

    private LocalCache<String> cache;

    @BeforeEach
    void setUp() {
        CacheConfig config = CacheConfig.defaultConfig()
                .maximumSize(100)
                .defaultTtl(60000)
                .cacheNull(true);
        cache = new LocalCache<>("test-cache", config);
    }

    /**
     * 基本操作测试。
     * <p>
     * 测试缓存的存取、删除和清空等基本操作。
     * </p>
     */
    @Nested
    @DisplayName("基本操作测试")
    class BasicOperationTests {

        /**
         * 测试正确存取缓存值。
         */
        @Test
        @DisplayName("应该正确存取缓存值")
        void shouldPutAndGet() {
            // when
            cache.put("key1", "value1");

            // then
            assertThat(cache.get("key1")).isEqualTo("value1");
        }

        /**
         * 测试获取不存在的键返回 null。
         */
        @Test
        @DisplayName("获取不存在的键应该返回 null")
        void shouldReturnNullForMissingKey() {
            assertThat(cache.get("non-existent")).isNull();
        }

        /**
         * 测试正确删除缓存。
         */
        @Test
        @DisplayName("应该正确删除缓存")
        void shouldEvict() {
            // given
            cache.put("key1", "value1");

            // when
            cache.evict("key1");

            // then
            assertThat(cache.get("key1")).isNull();
        }

        /**
         * 测试正确清空缓存。
         */
        @Test
        @DisplayName("应该正确清空缓存")
        void shouldClear() {
            // given
            cache.put("key1", "value1");
            cache.put("key2", "value2");

            // when
            cache.clear();

            // then
            assertThat(cache.get("key1")).isNull();
            assertThat(cache.get("key2")).isNull();
        }
    }

    /**
     * Null 值处理测试。
     * <p>
     * 测试缓存对 null 值的处理，包括缓存和不缓存 null 值的场景。
     * </p>
     */
    @Nested
    @DisplayName("Null 值处理测试")
    class NullValueTests {

        /**
         * 测试缓存 null 值。
         */
        @Test
        @DisplayName("应该缓存 null 值")
        void shouldCacheNullValue() {
            // when
            cache.put("null-key", null);

            // then
            assertThat(cache.containsKey("null-key")).isTrue();
            assertThat(cache.get("null-key")).isNull();
        }

        /**
         * 测试不缓存 null 值时键不存在。
         */
        @Test
        @DisplayName("不缓存 null 值时不应该存在键")
        void shouldNotCacheNullWhenDisabled() {
            // given
            CacheConfig config = CacheConfig.defaultConfig().cacheNull(false);
            LocalCache<String> noNullCache = new LocalCache<>("no-null-cache", config);

            // when
            noNullCache.put("null-key", null);

            // then
            assertThat(noNullCache.containsKey("null-key")).isFalse();
        }
    }

    /**
     * putIfAbsent 测试。
     * <p>
     * 测试仅在键不存在时设置值的操作。
     * </p>
     */
    @Nested
    @DisplayName("putIfAbsent 测试")
    class PutIfAbsentTests {

        /**
         * 测试键不存在时存入并返回 null。
         */
        @Test
        @DisplayName("键不存在时应该存入并返回 null")
        void shouldPutIfAbsent() {
            // when
            String result = cache.putIfAbsent("key1", "value1");

            // then
            assertThat(result).isNull();
            assertThat(cache.get("key1")).isEqualTo("value1");
        }

        /**
         * 测试键已存在时返回已存在的值。
         */
        @Test
        @DisplayName("键已存在时应该返回已存在的值")
        void shouldReturnExistingValue() {
            // given
            cache.put("key1", "value1");

            // when
            String result = cache.putIfAbsent("key1", "value2");

            // then
            assertThat(result).isEqualTo("value1");
            assertThat(cache.get("key1")).isEqualTo("value1");
        }
    }

    /**
     * getOrLoad 测试。
     * <p>
     * 测试缓存未命中时自动加载数据的功能。
     * </p>
     */
    @Nested
    @DisplayName("getOrLoad 测试")
    class GetOrLoadTests {

        /**
         * 测试缓存命中时返回缓存值。
         */
        @Test
        @DisplayName("缓存命中时应该返回缓存值")
        void shouldReturnCachedValue() {
            // given
            cache.put("key1", "value1");

            // when
            String result = cache.getOrLoad("key1", () -> "loaded-value");

            // then
            assertThat(result).isEqualTo("value1");
        }

        /**
         * 测试缓存未命中时加载并缓存。
         */
        @Test
        @DisplayName("缓存未命中时应该加载并缓存")
        void shouldLoadAndCache() {
            // when
            String result = cache.getOrLoad("key1", () -> "loaded-value");

            // then
            assertThat(result).isEqualTo("loaded-value");
            assertThat(cache.get("key1")).isEqualTo("loaded-value");
        }
    }

    /**
     * 缓存指标测试。
     * <p>
     * 测试缓存命中率和未命中率的记录和计算。
     * </p>
     */
    @Nested
    @DisplayName("缓存指标测试")
    class MetricsTests {

        /**
         * 测试正确记录命中和未命中。
         */
        @Test
        @DisplayName("应该正确记录命中和未命中")
        void shouldRecordHitAndMiss() {
            // given
            cache.put("key1", "value1");

            // when
            cache.get("key1"); // hit
            cache.get("key2"); // miss

            // then
            CacheMetrics metrics = cache.getMetrics();
            assertThat(metrics.getHitCount()).isEqualTo(1);
            assertThat(metrics.getMissCount()).isEqualTo(1);
        }

        /**
         * 测试正确计算命中率。
         */
        @Test
        @DisplayName("应该正确计算命中率")
        void shouldCalculateHitRate() {
            // given
            cache.put("key1", "value1");

            // when
            cache.get("key1"); // hit
            cache.get("key1"); // hit
            cache.get("key2"); // miss

            // then
            CacheMetrics metrics = cache.getMetrics();
            assertThat(metrics.getHitRate()).isEqualTo(2.0 / 3.0);
        }
    }

    /**
     * 缓存大小测试。
     * <p>
     * 测试缓存大小的获取。
     * </p>
     */
    @Nested
    @DisplayName("缓存大小测试")
    class SizeTests {

        /**
         * 测试正确返回缓存大小。
         */
        @Test
        @DisplayName("应该正确返回缓存大小")
        void shouldReturnCorrectSize() {
            // given
            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("key3", "value3");

            // when
            long size = cache.size();

            // then
            assertThat(size).isEqualTo(3);
        }

        /**
         * 测试删除后正确返回缓存大小。
         */
        @Test
        @DisplayName("删除后应该正确返回缓存大小")
        void shouldReturnCorrectSizeAfterEvict() {
            // given
            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.evict("key1");

            // when
            long size = cache.size();

            // then
            assertThat(size).isEqualTo(1);
        }
    }

    /**
     * createDefault 测试。
     * <p>
     * 测试静态工厂方法创建默认配置的缓存。
     * </p>
     */
    @Nested
    @DisplayName("createDefault 测试")
    class CreateDefaultTests {

        /**
         * 测试创建默认配置的缓存。
         */
        @Test
        @DisplayName("应该创建默认配置的缓存")
        void shouldCreateDefaultCache() {
            // when
            LocalCache<String> defaultCache = LocalCache.createDefault("default-cache");

            // then
            assertThat(defaultCache.getName()).isEqualTo("default-cache");
            assertThat(defaultCache.getConfig()).isNotNull();
        }
    }
}