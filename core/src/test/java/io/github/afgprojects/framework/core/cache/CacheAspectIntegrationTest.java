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
 * CacheAspect 集成测试
 */
@DisplayName("CacheAspect 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.cache.enabled=true",
                "afg.cache.type=local",
                "afg.cache.local.maximum-size=1000"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CacheAspectIntegrationTest {

    @Autowired(required = false)
    private DefaultCacheManager cacheManager;

    @Autowired(required = false)
    private CacheAspect cacheAspect;

    @Nested
    @DisplayName("CacheAspect 配置测试")
    class CacheAspectConfigTests {

        @Test
        @DisplayName("应该正确配置 CacheAspect")
        void shouldConfigureCacheAspect() {
            assertThat(cacheAspect).isNotNull();
        }

        @Test
        @DisplayName("应该正确配置 CacheManager")
        void shouldConfigureCacheManager() {
            assertThat(cacheManager).isNotNull();
        }
    }

    @Nested
    @DisplayName("缓存操作测试")
    class CacheOperationTests {

        @Test
        @DisplayName("应该能够操作本地缓存")
        void shouldOperateLocalCache() {
            AfgCache<String> cache = cacheManager.getCache("test-cache");

            cache.put("key1", "value1");
            assertThat(cache.get("key1")).isEqualTo("value1");

            cache.evict("key1");
            assertThat(cache.get("key1")).isNull();
        }

        @Test
        @DisplayName("应该支持 TTL")
        void shouldSupportTtl() {
            AfgCache<String> cache = cacheManager.getCache("ttl-cache");

            cache.put("key1", "value1", 1000);
            assertThat(cache.get("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("应该支持 containsKey")
        void shouldSupportContainsKey() {
            AfgCache<String> cache = cacheManager.getCache("contains-cache");

            cache.put("key1", "value1");
            assertThat(cache.containsKey("key1")).isTrue();
            assertThat(cache.containsKey("key2")).isFalse();
        }

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
