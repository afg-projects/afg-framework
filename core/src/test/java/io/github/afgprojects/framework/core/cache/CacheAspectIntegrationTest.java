package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * CacheAspect 集成测试。
 * <p>
 * 测试缓存切面与 Spring Boot 的集成，包括切面配置和缓存操作功能。
 * </p>
 *
 * @see CacheAspect
 */
@DisplayName("CacheAspect 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.core.cache.enabled=true",
                "afg.core.cache.type=local",
                "afg.core.cache.local.maximum-size=1000"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CacheAspectIntegrationTest {

    @Autowired(required = false)
    private DefaultCacheManager cacheManager;

    @Autowired(required = false)
    private CacheAspect cacheAspect;

    /**
     * CacheAspect 配置测试。
     * <p>
     * 测试切面和缓存管理器的自动配置。
     * </p>
     */
    @Nested
    @DisplayName("CacheAspect 配置测试")
    class CacheAspectConfigTests {

        /**
         * 测试正确配置 CacheAspect。
         */
        @Test
        @DisplayName("应该正确配置 CacheAspect")
        void shouldConfigureCacheAspect() {
            assertThat(cacheAspect).isNotNull();
        }

        /**
         * 测试正确配置 CacheManager。
         */
        @Test
        @DisplayName("应该正确配置 CacheManager")
        void shouldConfigureCacheManager() {
            assertThat(cacheManager).isNotNull();
        }
    }

    /**
     * 缓存操作测试。
     * <p>
     * 测试本地缓存的基本操作，包括存取、TTL 支持、containsKey 和 clear。
     * </p>
     */
    @Nested
    @DisplayName("缓存操作测试")
    class CacheOperationTests {

        /**
         * 测试能够操作本地缓存。
         */
        @Test
        @DisplayName("应该能够操作本地缓存")
        void shouldOperateLocalCache() {
            AfgCache<String> cache = cacheManager.getCache("test-cache");

            cache.put("key1", "value1");
            assertThat(cache.get("key1")).isEqualTo("value1");

            cache.evict("key1");
            assertThat(cache.get("key1")).isNull();
        }

        /**
         * 测试支持 TTL。
         */
        @Test
        @DisplayName("应该支持 TTL")
        void shouldSupportTtl() {
            AfgCache<String> cache = cacheManager.getCache("ttl-cache");

            cache.put("key1", "value1", 1000);
            assertThat(cache.get("key1")).isEqualTo("value1");
        }

        /**
         * 测试支持 containsKey。
         */
        @Test
        @DisplayName("应该支持 containsKey")
        void shouldSupportContainsKey() {
            AfgCache<String> cache = cacheManager.getCache("contains-cache");

            cache.put("key1", "value1");
            assertThat(cache.containsKey("key1")).isTrue();
            assertThat(cache.containsKey("key2")).isFalse();
        }

        /**
         * 测试支持 clear。
         */
        @Test
        @DisplayName("应该支持 clear")
        void shouldSupportClear() {
            AfgCache<String> cache = cacheManager.getCache("clear-cache");

            cache.put("key1", "value1");
            cache.put("key2", "value2");
            assertThat(cache.size()).isGreaterThanOrEqualTo(2);

            cache.clear();
            assertThat(cache.get("key1")).isNull();
            assertThat(cache.get("key2")).isNull();
        }
    }
}
