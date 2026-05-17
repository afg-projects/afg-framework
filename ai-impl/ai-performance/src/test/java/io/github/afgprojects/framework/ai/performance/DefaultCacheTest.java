package io.github.afgprojects.framework.ai.performance;

import io.github.afgprojects.framework.ai.core.performance.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultCache 单元测试
 */
class DefaultCacheTest {

    private DefaultCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new DefaultCache<>(100, Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("存储和获取缓存值")
    void putAndGet() {
        cache.put("key1", "value1");

        Optional<String> value = cache.get("key1");

        assertThat(value).isPresent().contains("value1");
    }

    @Test
    @DisplayName("获取不存在的缓存值")
    void get_notFound() {
        Optional<String> value = cache.get("non-existent");

        assertThat(value).isEmpty();
    }

    @Test
    @DisplayName("缓存加载器")
    void get_withLoader() {
        String value = cache.get("key1", k -> "loaded-" + k);

        assertThat(value).isEqualTo("loaded-key1");

        // 再次获取应该从缓存读取
        Optional<String> cached = cache.get("key1");
        assertThat(cached).isPresent().contains("loaded-key1");
    }

    @Test
    @DisplayName("缓存统计")
    void getStats() {
        cache.put("key1", "value1");

        cache.get("key1"); // hit
        cache.get("key1"); // hit
        cache.get("key2"); // miss

        Cache.CacheStats stats = cache.getStats();

        assertThat(stats.getHitCount()).isEqualTo(2);
        assertThat(stats.getMissCount()).isEqualTo(1);
        assertThat(stats.getHitRate()).isCloseTo(0.666, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("删除缓存值")
    void remove() {
        cache.put("key1", "value1");

        Optional<String> removed = cache.remove("key1");

        assertThat(removed).isPresent().contains("value1");
        assertThat(cache.containsKey("key1")).isFalse();
    }

    @Test
    @DisplayName("检查是否存在")
    void containsKey() {
        cache.put("key1", "value1");

        assertThat(cache.containsKey("key1")).isTrue();
        assertThat(cache.containsKey("key2")).isFalse();
    }

    @Test
    @DisplayName("清空缓存")
    void clear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        cache.clear();

        assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("LRU 驱逐")
    void lruEviction() {
        cache = new DefaultCache<>(3, Duration.ofMinutes(10));

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4"); // 应该驱逐 key1

        assertThat(cache.containsKey("key1")).isFalse();
        assertThat(cache.containsKey("key4")).isTrue();
        assertThat(cache.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("TTL 过期")
    void ttlExpiration() throws InterruptedException {
        cache = new DefaultCache<>(100, Duration.ofMillis(100));

        cache.put("key1", "value1");

        assertThat(cache.get("key1")).isPresent();

        Thread.sleep(150);

        assertThat(cache.get("key1")).isEmpty();
    }

    @Test
    @DisplayName("putIfAbsent")
    void putIfAbsent() {
        cache.put("key1", "value1");

        String existing = cache.putIfAbsent("key1", "value2");

        assertThat(existing).isEqualTo("value1");
        assertThat(cache.get("key1")).isPresent().contains("value1");

        String result = cache.putIfAbsent("key2", "value2");

        assertThat(result).isNull();
        assertThat(cache.get("key2")).isPresent().contains("value2");
    }

    @Test
    @DisplayName("条件失效")
    void invalidate() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "other");

        long count = cache.invalidate((k, v) -> v.startsWith("value"));

        assertThat(count).isEqualTo(2);
        assertThat(cache.size()).isEqualTo(1);
    }
}