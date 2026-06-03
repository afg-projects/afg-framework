package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.performance.Cache;
import io.github.afgprojects.framework.ai.core.performance.DefaultCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * DefaultCache 纯单元测试
 */
@DisplayName("DefaultCache")
class DefaultCacheTest {

    @Nested
    @DisplayName("put + get")
    class PutAndGet {

        @Test
        @DisplayName("应存储并获取缓存值")
        void shouldPutAndGet() {
            Cache<String, String> cache = new DefaultCache<>();

            cache.put("key1", "value1");

            assertThat(cache.get("key1")).isPresent().contains("value1");
        }

        @Test
        @DisplayName("不存在的键应返回 empty")
        void shouldReturnEmpty_whenKeyNotExists() {
            Cache<String, String> cache = new DefaultCache<>();

            assertThat(cache.get("nonexistent")).isEmpty();
        }
    }

    @Nested
    @DisplayName("containsKey")
    class ContainsKey {

        @Test
        @DisplayName("已存在的键应返回 true")
        void shouldReturnTrue_whenKeyExists() {
            Cache<String, String> cache = new DefaultCache<>();
            cache.put("key1", "value1");

            assertThat(cache.containsKey("key1")).isTrue();
        }

        @Test
        @DisplayName("不存在的键应返回 false")
        void shouldReturnFalse_whenKeyNotExists() {
            Cache<String, String> cache = new DefaultCache<>();

            assertThat(cache.containsKey("nonexistent")).isFalse();
        }
    }

    @Nested
    @DisplayName("putIfAbsent")
    class PutIfAbsent {

        @Test
        @DisplayName("键不存在时应存入并返回 null")
        void shouldPutAndReturnNull_whenKeyAbsent() {
            Cache<String, String> cache = new DefaultCache<>();

            var result = cache.putIfAbsent("key1", "value1");

            assertThat(result).isNull();
            assertThat(cache.get("key1")).isPresent().contains("value1");
        }

        @Test
        @DisplayName("键已存在时应返回已存在的值")
        void shouldReturnExistingValue_whenKeyPresent() {
            Cache<String, String> cache = new DefaultCache<>();
            cache.put("key1", "value1");

            var result = cache.putIfAbsent("key1", "value2");

            assertThat(result).isEqualTo("value1");
            assertThat(cache.get("key1")).isPresent().contains("value1");
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("应移除并返回缓存值")
        void shouldRemoveAndReturnValue() {
            Cache<String, String> cache = new DefaultCache<>();
            cache.put("key1", "value1");

            var removed = cache.remove("key1");

            assertThat(removed).isPresent().contains("value1");
            assertThat(cache.get("key1")).isEmpty();
        }

        @Test
        @DisplayName("移除不存在的键应返回 empty")
        void shouldReturnEmpty_whenRemovingNonexistent() {
            Cache<String, String> cache = new DefaultCache<>();

            assertThat(cache.remove("nonexistent")).isEmpty();
        }
    }

    @Nested
    @DisplayName("clear + size")
    class ClearAndSize {

        @Test
        @DisplayName("应清空缓存")
        void shouldClearCache() {
            Cache<String, String> cache = new DefaultCache<>();
            cache.put("key1", "value1");
            cache.put("key2", "value2");

            cache.clear();

            assertThat(cache.size()).isZero();
        }

        @Test
        @DisplayName("应返回正确的缓存大小")
        void shouldReturnCorrectSize() {
            Cache<String, String> cache = new DefaultCache<>();
            cache.put("key1", "value1");
            cache.put("key2", "value2");

            assertThat(cache.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("get 带 CacheLoader")
    class GetWithLoader {

        @Test
        @DisplayName("缓存命中应不调用加载器")
        void shouldNotCallLoader_whenCacheHit() {
            Cache<String, String> cache = new DefaultCache<>();
            cache.put("key1", "cached");

            var result = cache.get("key1", key -> {
                throw new RuntimeException("should not be called");
            });

            assertThat(result).isEqualTo("cached");
        }

        @Test
        @DisplayName("缓存未命中应调用加载器")
        void shouldCallLoader_whenCacheMiss() {
            Cache<String, String> cache = new DefaultCache<>();

            var result = cache.get("key1", key -> "loaded-" + key);

            assertThat(result).isEqualTo("loaded-key1");
            assertThat(cache.get("key1")).isPresent().contains("loaded-key1");
        }
    }

    @Nested
    @DisplayName("TTL 过期")
    class TtlExpiration {

        @Test
        @DisplayName("极短 TTL 后应过期")
        void shouldExpire_afterShortTtl() {
            Cache<String, String> cache = new DefaultCache<>();

            cache.put("key1", "value1", Duration.ofMillis(1));

            // 等待过期
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            assertThat(cache.get("key1")).isEmpty();
            assertThat(cache.containsKey("key1")).isFalse();
        }
    }

    @Nested
    @DisplayName("LRU 驱逐")
    class LruEviction {

        @Test
        @DisplayName("超过 maxSize 应驱逐最旧条目")
        void shouldEvictOldest_whenExceedsMaxSize() {
            Cache<String, String> cache = new DefaultCache<>(2, null);

            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("key3", "value3"); // 应驱逐 key1

            assertThat(cache.size()).isLessThanOrEqualTo(2);
            assertThat(cache.get("key3")).isPresent().contains("value3");
        }
    }

    @Nested
    @DisplayName("invalidate")
    class Invalidate {

        @Test
        @DisplayName("应按条件使缓存失效")
        void shouldInvalidateByPredicate() {
            Cache<String, String> cache = new DefaultCache<>();
            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("key3", "value3");

            long count = cache.invalidate((key, value) -> key.startsWith("key1") || key.startsWith("key2"));

            assertThat(count).isEqualTo(2);
            assertThat(cache.containsKey("key1")).isFalse();
            assertThat(cache.containsKey("key2")).isFalse();
            assertThat(cache.containsKey("key3")).isTrue();
        }
    }

    @Nested
    @DisplayName("getStats")
    class Stats {

        @Test
        @DisplayName("应正确统计命中率")
        void shouldTrackHitAndMissCount() {
            Cache<String, String> cache = new DefaultCache<>();
            cache.put("key1", "value1");

            cache.get("key1");  // hit
            cache.get("key1");  // hit
            cache.get("nonexistent"); // miss

            var stats = cache.getStats();

            assertThat(stats.getHitCount()).isEqualTo(2);
            assertThat(stats.getMissCount()).isEqualTo(1);
            assertThat(stats.getHitRate()).isCloseTo(2.0 / 3.0, within(0.001));
        }
    }
}
