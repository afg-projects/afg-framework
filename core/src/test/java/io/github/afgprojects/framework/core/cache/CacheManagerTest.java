package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.afgprojects.framework.core.cache.exception.CacheException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DefaultCacheManager 测试
 */
@DisplayName("DefaultCacheManager 测试")
class CacheManagerTest {

    private DefaultCacheManager cacheManager;
    private CacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CacheProperties();
        properties.setEnabled(true);
        properties.setType(CacheProperties.CacheType.LOCAL);
        cacheManager = new DefaultCacheManager(properties);
    }

    @Nested
    @DisplayName("获取缓存测试")
    class GetCacheTests {

        @Test
        @DisplayName("应该创建并返回缓存")
        void shouldCreateAndReturnCache() {
            // when
            AfgCache<String> cache = cacheManager.getCache("test-cache");

            // then
            assertThat(cache).isNotNull();
            assertThat(cache.getName()).isEqualTo("test-cache");
        }

        @Test
        @DisplayName("重复获取应该返回同一缓存实例")
        void shouldReturnSameCacheInstance() {
            // when
            AfgCache<String> cache1 = cacheManager.getCache("test-cache");
            AfgCache<String> cache2 = cacheManager.getCache("test-cache");

            // then
            assertThat(cache1).isSameAs(cache2);
        }

        @Test
        @DisplayName("不同名称应该返回不同缓存实例")
        void shouldReturnDifferentCacheInstances() {
            // when
            AfgCache<String> cache1 = cacheManager.getCache("cache1");
            AfgCache<String> cache2 = cacheManager.getCache("cache2");

            // then
            assertThat(cache1).isNotSameAs(cache2);
        }
    }

    @Nested
    @DisplayName("获取特定类型缓存测试")
    class GetTypedCacheTests {

        @Test
        @DisplayName("应该返回本地缓存")
        void shouldReturnLocalCache() {
            // when
            LocalCache<String> cache = cacheManager.getLocalCache("test-cache");

            // then
            assertThat(cache).isNotNull();
            assertThat(cache).isInstanceOf(LocalCache.class);
        }

        @Test
        @DisplayName("没有 Redisson 时获取分布式缓存应该抛异常")
        void shouldThrowExceptionForDistributedCacheWithoutRedisson() {
            assertThatThrownBy(() -> cacheManager.getDistributedCache("test-cache"))
                    .isInstanceOf(CacheException.class)
                    .hasMessageContaining("RedissonClient is not configured");
        }

        @Test
        @DisplayName("没有 Redisson 时获取多级缓存应该抛异常")
        void shouldThrowExceptionForMultiLevelCacheWithoutRedisson() {
            assertThatThrownBy(() -> cacheManager.getMultiLevelCache("test-cache"))
                    .isInstanceOf(CacheException.class)
                    .hasMessageContaining("RedissonClient is not configured");
        }
    }

    @Nested
    @DisplayName("缓存管理测试")
    class CacheManagementTests {

        @Test
        @DisplayName("应该正确注册缓存")
        void shouldRegisterCache() {
            // given
            LocalCache<String> cache = LocalCache.createDefault("custom-cache");

            // when
            cacheManager.registerCache("custom-cache", cache);

            // then
            assertThat(cacheManager.containsCache("custom-cache")).isTrue();
            assertThat(cacheManager.getCache("custom-cache")).isSameAs(cache);
        }

        @Test
        @DisplayName("应该正确移除缓存")
        void shouldRemoveCache() {
            // given
            cacheManager.getCache("test-cache");

            // when
            cacheManager.removeCache("test-cache");

            // then
            assertThat(cacheManager.containsCache("test-cache")).isFalse();
        }

        @Test
        @DisplayName("移除缓存时应该清空缓存内容")
        void shouldClearCacheWhenRemove() {
            // given
            AfgCache<String> cache = cacheManager.getCache("test-cache");
            cache.put("key1", "value1");

            // when
            cacheManager.removeCache("test-cache");

            // then
            // 再次获取时是新的缓存实例
            AfgCache<String> newCache = cacheManager.getCache("test-cache");
            assertThat(newCache.get("key1")).isNull();
        }
    }

    @Nested
    @DisplayName("缓存名称测试")
    class CacheNamesTests {

        @Test
        @DisplayName("应该返回所有缓存名称")
        void shouldReturnAllCacheNames() {
            // given
            cacheManager.getCache("cache1");
            cacheManager.getCache("cache2");
            cacheManager.getCache("cache3");

            // when
            java.util.Set<String> names = cacheManager.getCacheNames();

            // then
            assertThat(names).containsExactlyInAnyOrder("cache1", "cache2", "cache3");
        }
    }

    @Nested
    @DisplayName("清空和销毁测试")
    class ClearAndDestroyTests {

        @Test
        @DisplayName("清空所有缓存应该清除所有内容")
        void shouldClearAllCaches() {
            // given
            AfgCache<String> cache1 = cacheManager.getCache("cache1");
            AfgCache<String> cache2 = cacheManager.getCache("cache2");
            cache1.put("key1", "value1");
            cache2.put("key2", "value2");

            // when
            cacheManager.clearAll();

            // then
            // 清空后重新获取的缓存应该是同一实例（已清空）
            assertThat(cache1.get("key1")).isNull();
            assertThat(cache2.get("key2")).isNull();
        }

        @Test
        @DisplayName("销毁应该清空并移除所有缓存")
        void shouldDestroyAllCaches() {
            // given
            cacheManager.getCache("cache1");
            cacheManager.getCache("cache2");

            // when
            cacheManager.destroy();

            // then
            assertThat(cacheManager.getCacheNames()).isEmpty();
        }
    }
}