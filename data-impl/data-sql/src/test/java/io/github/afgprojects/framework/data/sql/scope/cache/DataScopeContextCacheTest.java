package io.github.afgprojects.framework.data.sql.scope.cache;

import io.github.afgprojects.framework.data.sql.scope.DataScopeUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeContextCache 测试
 */
class DataScopeContextCacheTest {

    private DataScopeContextCache cache;

    @AfterEach
    void tearDown() {
        if (cache != null) {
            cache.shutdown();
        }
    }

    @Test
    @DisplayName("缓存命中")
    void testCacheHit() {
        cache = new DataScopeContextCache(Duration.ofMinutes(5), false);

        AtomicInteger loadCount = new AtomicInteger(0);
        DataScopeContextCache.ContextLoader loader = () -> {
            loadCount.incrementAndGet();
            return DataScopeUserContext.builder().userId(123L).build();
        };

        // 第一次加载
        DataScopeUserContext result1 = cache.getOrCreate(123L, loader);
        assertThat(result1.getUserId()).isEqualTo(123L);
        assertThat(loadCount.get()).isEqualTo(1);

        // 第二次从缓存获取
        DataScopeUserContext result2 = cache.getOrCreate(123L, loader);
        assertThat(result2.getUserId()).isEqualTo(123L);
        assertThat(loadCount.get()).isEqualTo(1); // 没有增加
    }

    @Test
    @DisplayName("缓存过期")
    void testCacheExpire() throws InterruptedException {
        cache = new DataScopeContextCache(Duration.ofMillis(100), false);

        AtomicInteger loadCount = new AtomicInteger(0);
        DataScopeContextCache.ContextLoader loader = () -> {
            loadCount.incrementAndGet();
            return DataScopeUserContext.builder().userId(123L).build();
        };

        // 第一次加载
        cache.getOrCreate(123L, loader);
        assertThat(loadCount.get()).isEqualTo(1);

        // 等待过期
        Thread.sleep(150);

        // 过期后重新加载
        cache.getOrCreate(123L, loader);
        assertThat(loadCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("手动清除缓存")
    void testInvalidate() {
        cache = new DataScopeContextCache(Duration.ofMinutes(5), false);

        AtomicInteger loadCount = new AtomicInteger(0);
        DataScopeContextCache.ContextLoader loader = () -> {
            loadCount.incrementAndGet();
            return DataScopeUserContext.builder().userId(123L).build();
        };

        // 第一次加载
        cache.getOrCreate(123L, loader);
        assertThat(loadCount.get()).isEqualTo(1);

        // 清除缓存
        cache.invalidate(123L);

        // 再次加载
        cache.getOrCreate(123L, loader);
        assertThat(loadCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("清除所有缓存")
    void testInvalidateAll() {
        cache = new DataScopeContextCache(Duration.ofMinutes(5), false);

        AtomicInteger loadCount = new AtomicInteger(0);
        DataScopeContextCache.ContextLoader loader = () -> {
            loadCount.incrementAndGet();
            return DataScopeUserContext.builder().userId(123L).build();
        };

        cache.getOrCreate(1L, loader);
        cache.getOrCreate(2L, loader);
        assertThat(loadCount.get()).isEqualTo(2);
        assertThat(cache.size()).isEqualTo(2);

        cache.invalidateAll();
        assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("put 和 get 方法")
    void testPutAndGet() {
        cache = new DataScopeContextCache(Duration.ofMinutes(5), false);

        DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .deptId(456L)
                .build();

        cache.put(123L, context);

        DataScopeUserContext result = cache.get(123L);
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(123L);
        assertThat(result.getDeptId()).isEqualTo(456L);
    }

    @Test
    @DisplayName("get 返回 null 当缓存不存在")
    void testGetNotExists() {
        cache = new DataScopeContextCache(Duration.ofMinutes(5), false);

        DataScopeUserContext result = cache.get(999L);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("缓存大小")
    void testSize() {
        cache = new DataScopeContextCache(Duration.ofMinutes(5), false);

        assertThat(cache.size()).isZero();

        cache.put(1L, DataScopeUserContext.empty());
        cache.put(2L, DataScopeUserContext.empty());

        assertThat(cache.size()).isEqualTo(2);
    }
}