package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.github.afgprojects.framework.core.support.RedisContainerSingleton;
import io.github.afgprojects.framework.core.support.TestApplication;
import org.springframework.test.annotation.DirtiesContext;

/**
 * 缓存自动配置集成测试
 */
@DisplayName("缓存自动配置集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.cache.enabled=true",
                "afg.cache.type=local",
                "afg.cache.default-ttl=60000",
                "afg.cache.local.maximum-size=1000"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CacheAutoConfigurationIntegrationTest {

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", RedisContainerSingleton::getHost);
        registry.add("spring.data.redis.port", RedisContainerSingleton::getPort);
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired(required = false)
    private DefaultCacheManager cacheManager;

    @Autowired(required = false)
    private CacheAspect cacheAspect;

    @Autowired(required = false)
    private CacheProperties cacheProperties;

    @Nested
    @DisplayName("自动配置测试")
    class AutoConfigurationTests {

        @Test
        @DisplayName("应该自动配置 CacheManager")
        void shouldAutoConfigureCacheManager() {
            assertThat(cacheManager).isNotNull();
        }

        @Test
        @DisplayName("应该自动配置 CacheAspect")
        void shouldAutoConfigureCacheAspect() {
            assertThat(cacheAspect).isNotNull();
        }

        @Test
        @DisplayName("应该自动配置 CacheProperties")
        void shouldAutoConfigureCacheProperties() {
            assertThat(cacheProperties).isNotNull();
            assertThat(cacheProperties.isEnabled()).isTrue();
            assertThat(cacheProperties.getType()).isEqualTo(CacheProperties.CacheType.LOCAL);
            assertThat(cacheProperties.getDefaultTtl()).isEqualTo(60000);
        }
    }

    @Nested
    @DisplayName("缓存操作集成测试")
    class CacheOperationIntegrationTests {

        @Test
        @DisplayName("应该能够创建和使用缓存")
        void shouldCreateAndUseCache() {
            // when
            AfgCache<String> cache = cacheManager.getCache("integration-test-cache");
            cache.put("key1", "value1");

            // then
            assertThat(cache.get("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("应该使用配置的默认 TTL")
        void shouldUseConfiguredDefaultTtl() {
            // when
            CacheConfig config = cacheProperties.toCacheConfig();

            // then
            assertThat(config.getDefaultTtl()).isEqualTo(60000);
        }
    }
}