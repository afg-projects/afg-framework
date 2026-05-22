package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cache.CacheAspect;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.core.cache.LocalCache;
import io.github.afgprojects.framework.core.cache.exception.CacheException;

/**
 * CacheAutoConfiguration 单元测试。
 * 测试缓存自动配置类的 Bean 创建和缓存管理器类型选择功能。
 *
 * @see CacheAutoConfiguration
 */
@DisplayName("CacheAutoConfiguration 测试")
class CacheAutoConfigurationTest {

    private CacheAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new CacheAutoConfiguration();
    }

    /**
     * 缓存管理器配置测试。
     * 验证 cacheManager Bean 的创建和不同缓存类型的处理。
     */
    @Nested
    @DisplayName("cacheManager 配置测试")
    class CacheManagerTests {

        /**
         * 测试创建本地缓存管理器。
         */
        @Test
        @DisplayName("应该创建本地缓存管理器")
        void shouldCreateLocalCacheManager() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getCache().setType(AfgCoreProperties.CacheConfig.CacheType.LOCAL);

            DefaultCacheManager manager = configuration.cacheManager(properties);

            assertThat(manager).isNotNull();
        }

        /**
         * 测试创建分布式缓存管理器（回退到本地缓存）。
         */
        @Test
        @DisplayName("应该创建分布式缓存管理器（回退到本地缓存）")
        void shouldCreateDistributedCacheManager() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getCache().setType(AfgCoreProperties.CacheConfig.CacheType.DISTRIBUTED);

            DefaultCacheManager manager = configuration.cacheManager(properties);

            assertThat(manager).isNotNull();
        }

        /**
         * 测试创建多级缓存管理器（回退到本地缓存）。
         */
        @Test
        @DisplayName("应该创建多级缓存管理器（回退到本地缓存）")
        void shouldCreateMultiLevelCacheManager() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getCache().setType(AfgCoreProperties.CacheConfig.CacheType.MULTI_LEVEL);

            DefaultCacheManager manager = configuration.cacheManager(properties);

            assertThat(manager).isNotNull();
        }

        /**
         * 测试当需要分布式缓存但没有 RedissonClient 时回退到本地缓存。
         */
        @Test
        @DisplayName("当需要分布式缓存但没有 RedissonClient 时应该回退到本地缓存")
        void shouldFallbackToLocalCacheWhenDistributedCacheNeededButNoRedissonClient() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getCache().setType(AfgCoreProperties.CacheConfig.CacheType.DISTRIBUTED);
            properties.getCache().setDefaultTtl(60000);

            DefaultCacheManager manager = configuration.cacheManager(properties);

            assertThat(manager).isNotNull();
            // 验证可以创建本地缓存
            LocalCache<String> cache = manager.getLocalCache("test");
            assertThat(cache).isNotNull();
        }

        /**
         * 测试当需要多级缓存但没有 RedissonClient 时回退到本地缓存。
         */
        @Test
        @DisplayName("当需要多级缓存但没有 RedissonClient 时应该回退到本地缓存")
        void shouldFallbackToLocalCacheWhenMultiLevelCacheNeededButNoRedissonClient() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getCache().setType(AfgCoreProperties.CacheConfig.CacheType.MULTI_LEVEL);

            DefaultCacheManager manager = configuration.cacheManager(properties);

            assertThat(manager).isNotNull();
            LocalCache<String> cache = manager.getLocalCache("test");
            assertThat(cache).isNotNull();
        }
    }

    /**
     * 缓存切面配置测试。
     * 验证 cacheAspect Bean 的创建。
     */
    @Nested
    @DisplayName("cacheAspect 配置测试")
    class CacheAspectTests {

        /**
         * 测试创建缓存切面。
         */
        @Test
        @DisplayName("应该创建缓存切面")
        void shouldCreateCacheAspect() {
            AfgCoreProperties properties = new AfgCoreProperties();
            DefaultCacheManager manager = new DefaultCacheManager(properties);

            CacheAspect aspect = configuration.cacheAspect(manager);

            assertThat(aspect).isNotNull();
        }
    }
}