package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cache.metrics.CacheMetrics;

/**
 * LocalCache 测试
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

    @Nested
    @DisplayName("基本操作测试")
    class BasicOperationTests {

        @Test
        @DisplayName("应该正确存取缓存值")
        void shouldPutAndGet() {
            // when
            cache.put("key1", "value1");

            // then
            assertThat(cache.get("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("获取不存在的键应该返回 null")
        void shouldReturnNullForMissingKey() {
            assertThat(cache.get("non-existent")).isNull();
        }

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

    @Nested
    @DisplayName("Null 值处理测试")
    class NullValueTests {

        @Test
        @DisplayName("应该缓存 null 值")
        void shouldCacheNullValue() {
            // when
            cache.put("null-key", null);

            // then
            assertThat(cache.containsKey("null-key")).isTrue();
            assertThat(cache.get("null-key")).isNull();
        }

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

    @Nested
    @DisplayName("putIfAbsent 测试")
    class PutIfAbsentTests {

        @Test
        @DisplayName("键不存在时应该存入并返回 null")
        void shouldPutIfAbsent() {
            // when
            String result = cache.putIfAbsent("key1", "value1");

            // then
            assertThat(result).isNull();
            assertThat(cache.get("key1")).isEqualTo("value1");
        }

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

    @Nested
    @DisplayName("getOrLoad 测试")
    class GetOrLoadTests {

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

    @Nested
    @DisplayName("缓存指标测试")
    class MetricsTests {

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

    @Nested
    @DisplayName("缓存大小测试")
    class SizeTests {

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

    @Nested
    @DisplayName("createDefault 测试")
    class CreateDefaultTests {

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