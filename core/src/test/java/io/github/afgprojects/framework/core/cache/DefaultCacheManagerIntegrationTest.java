package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.cache.exception.CacheException;
import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * DefaultCacheManager 集成测试
 */
@DisplayName("DefaultCacheManager 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.cache.enabled=true",
                "afg.cache.type=LOCAL",
                "afg.cache.default-ttl=60000"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DefaultCacheManagerIntegrationTest {

    @Autowired(required = false)
    private DefaultCacheManager cacheManager;

    @Autowired(required = false)
    private CacheProperties cacheProperties;

    @Nested
    @DisplayName("缓存管理器配置测试")
    class CacheManagerConfigTests {

        @Test
        @DisplayName("应该自动配置缓存管理器")
        void shouldAutoConfigureCacheManager() {
            assertThat(cacheManager).isNotNull();
        }

        @Test
        @DisplayName("应该自动配置缓存属性")
        void shouldAutoConfigureCacheProperties() {
            assertThat(cacheProperties).isNotNull();
            assertThat(cacheProperties.isEnabled()).isTrue();
            assertThat(cacheProperties.getType()).isEqualTo(CacheProperties.CacheType.LOCAL);
        }
    }

    @Nested
    @DisplayName("缓存操作测试")
    class CacheOperationTests {

        @Test
        @DisplayName("应该能够创建和获取缓存")
        void shouldCreateAndGetCache() {
            AfgCache<String> cache = cacheManager.getCache("test-cache");

            assertThat(cache).isNotNull();
            assertThat(cache.getName()).isEqualTo("test-cache");
        }

        @Test
        @DisplayName("应该能够存取缓存值")
        void shouldPutAndGetCacheValue() {
            AfgCache<String> cache = cacheManager.getCache("test-cache");

            cache.put("key1", "value1");
            assertThat(cache.get("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("应该能够删除缓存值")
        void shouldEvictCacheValue() {
            AfgCache<String> cache = cacheManager.getCache("test-cache");
            cache.put("key1", "value1");

            cache.evict("key1");

            assertThat(cache.get("key1")).isNull();
        }

        @Test
        @DisplayName("应该能够清空缓存")
        void shouldClearCache() {
            AfgCache<String> cache = cacheManager.getCache("test-cache");
            cache.put("key1", "value1");
            cache.put("key2", "value2");

            cache.clear();

            assertThat(cache.get("key1")).isNull();
            assertThat(cache.get("key2")).isNull();
        }

        @Test
        @DisplayName("应该能够检查缓存是否存在")
        void shouldCheckCacheExists() {
            cacheManager.getCache("existing-cache");

            assertThat(cacheManager.containsCache("existing-cache")).isTrue();
            assertThat(cacheManager.containsCache("non-existing-cache")).isFalse();
        }

        @Test
        @DisplayName("应该能够获取所有缓存名称")
        void shouldGetAllCacheNames() {
            cacheManager.getCache("cache1");
            cacheManager.getCache("cache2");

            var names = cacheManager.getCacheNames();

            assertThat(names).contains("cache1", "cache2");
        }
    }

    @Nested
    @DisplayName("本地缓存测试")
    class LocalCacheTests {

        @Test
        @DisplayName("应该能够获取本地缓存")
        void shouldGetLocalCache() {
            LocalCache<String> cache = cacheManager.getLocalCache("local-test");

            assertThat(cache).isNotNull();
            assertThat(cache.getName()).isEqualTo("local-test");
        }

        @Test
        @DisplayName("本地缓存应该支持 putIfAbsent")
        void shouldSupportPutIfAbsent() {
            LocalCache<String> cache = cacheManager.getLocalCache("local-test");

            String result1 = cache.putIfAbsent("key1", "value1");
            assertThat(result1).isNull();

            String result2 = cache.putIfAbsent("key1", "value2");
            assertThat(result2).isEqualTo("value1");
        }

        @Test
        @DisplayName("本地缓存应该支持 getOrLoad")
        void shouldSupportGetOrLoad() {
            LocalCache<String> cache = cacheManager.getLocalCache("local-test");

            String result = cache.getOrLoad("load-key", () -> "loaded-value");

            assertThat(result).isEqualTo("loaded-value");
            assertThat(cache.get("load-key")).isEqualTo("loaded-value");
        }

        @Test
        @DisplayName("本地缓存应该支持大小查询")
        void shouldSupportSizeQuery() {
            LocalCache<String> cache = cacheManager.getLocalCache("size-test");

            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("key3", "value3");

            assertThat(cache.size()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("分布式缓存测试")
    class DistributedCacheTests {

        @Test
        @DisplayName("没有 Redisson 时应该抛出异常")
        void shouldThrowExceptionWithoutRedisson() {
            assertThatThrownBy(() -> cacheManager.getDistributedCache("dist-test"))
                    .isInstanceOf(CacheException.class)
                    .hasMessageContaining("RedissonClient is not configured");
        }
    }

    @Nested
    @DisplayName("多级缓存测试")
    class MultiLevelCacheTests {

        @Test
        @DisplayName("没有 Redisson 时应该抛出异常")
        void shouldThrowExceptionWithoutRedisson() {
            assertThatThrownBy(() -> cacheManager.getMultiLevelCache("multi-test"))
                    .isInstanceOf(CacheException.class)
                    .hasMessageContaining("RedissonClient is not configured");
        }
    }

    @Nested
    @DisplayName("缓存管理测试")
    class CacheManagementTests {

        @Test
        @DisplayName("应该能够注册自定义缓存")
        void shouldRegisterCustomCache() {
            LocalCache<String> customCache = LocalCache.createDefault("custom-cache");

            cacheManager.registerCache("custom-cache", customCache);

            assertThat(cacheManager.containsCache("custom-cache")).isTrue();
            assertThat(cacheManager.getCache("custom-cache")).isSameAs(customCache);
        }

        @Test
        @DisplayName("应该能够移除缓存")
        void shouldRemoveCache() {
            cacheManager.getCache("to-remove");

            cacheManager.removeCache("to-remove");

            assertThat(cacheManager.containsCache("to-remove")).isFalse();
        }

        @Test
        @DisplayName("应该能够清空所有缓存")
        void shouldClearAllCaches() {
            AfgCache<String> cache1 = cacheManager.getCache("clear-test-1");
            AfgCache<String> cache2 = cacheManager.getCache("clear-test-2");
            cache1.put("key1", "value1");
            cache2.put("key2", "value2");

            cacheManager.clearAll();

            assertThat(cache1.get("key1")).isNull();
            assertThat(cache2.get("key2")).isNull();
        }
    }
}
