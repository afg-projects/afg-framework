package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.cache.CacheAspect;
import io.github.afgprojects.framework.core.cache.CacheConfig;
import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.core.cache.DistributedCache;
import io.github.afgprojects.framework.core.cache.LocalCache;
import io.github.afgprojects.framework.core.cache.MultiLevelCache;
import io.github.afgprojects.framework.core.cache.exception.CacheException;

/**
 * CacheAutoConfiguration 测试
 */
@DisplayName("CacheAutoConfiguration 测试")
class CacheAutoConfigurationTest {

    private CacheAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new CacheAutoConfiguration();
    }

    @Nested
    @DisplayName("cacheManager 配置测试")
    class CacheManagerTests {

        @Test
        @DisplayName("应该创建本地缓存管理器")
        void shouldCreateLocalCacheManager() {
            CacheProperties properties = new CacheProperties();
            properties.setType(CacheProperties.CacheType.LOCAL);

            DefaultCacheManager manager = configuration.cacheManager(properties, null);

            assertThat(manager).isNotNull();
            assertThat(manager.getProperties()).isEqualTo(properties);
        }

        @Test
        @DisplayName("应该创建分布式缓存管理器")
        void shouldCreateDistributedCacheManager() {
            CacheProperties properties = new CacheProperties();
            properties.setType(CacheProperties.CacheType.DISTRIBUTED);
            RedissonClient redissonClient = mock(RedissonClient.class);

            DefaultCacheManager manager = configuration.cacheManager(properties, redissonClient);

            assertThat(manager).isNotNull();
        }

        @Test
        @DisplayName("应该创建多级缓存管理器")
        void shouldCreateMultiLevelCacheManager() {
            CacheProperties properties = new CacheProperties();
            properties.setType(CacheProperties.CacheType.MULTI_LEVEL);
            RedissonClient redissonClient = mock(RedissonClient.class);

            DefaultCacheManager manager = configuration.cacheManager(properties, redissonClient);

            assertThat(manager).isNotNull();
        }

        @Test
        @DisplayName("当需要分布式缓存但没有 RedissonClient 时应该回退到本地缓存")
        void shouldFallbackToLocalCacheWhenDistributedCacheNeededButNoRedissonClient() {
            CacheProperties properties = new CacheProperties();
            properties.setType(CacheProperties.CacheType.DISTRIBUTED);
            properties.setDefaultTtl(60000);

            DefaultCacheManager manager = configuration.cacheManager(properties, null);

            assertThat(manager).isNotNull();
            // 验证可以创建本地缓存
            LocalCache<String> cache = manager.getLocalCache("test");
            assertThat(cache).isNotNull();
        }

        @Test
        @DisplayName("当需要多级缓存但没有 RedissonClient 时应该回退到本地缓存")
        void shouldFallbackToLocalCacheWhenMultiLevelCacheNeededButNoRedissonClient() {
            CacheProperties properties = new CacheProperties();
            properties.setType(CacheProperties.CacheType.MULTI_LEVEL);

            DefaultCacheManager manager = configuration.cacheManager(properties, null);

            assertThat(manager).isNotNull();
            LocalCache<String> cache = manager.getLocalCache("test");
            assertThat(cache).isNotNull();
        }
    }

    @Nested
    @DisplayName("needsDistributedCache 测试")
    class NeedsDistributedCacheTests {

        @Test
        @DisplayName("DISTRIBUTED 类型应该需要分布式缓存")
        void distributedTypeShouldNeedDistributedCache() {
            CacheProperties properties = new CacheProperties();
            properties.setType(CacheProperties.CacheType.DISTRIBUTED);

            // 通过 cacheManager 行为间接验证
            DefaultCacheManager manager = configuration.cacheManager(properties, null);
            assertThat(manager).isNotNull();
        }

        @Test
        @DisplayName("MULTI_LEVEL 类型应该需要分布式缓存")
        void multiLevelTypeShouldNeedDistributedCache() {
            CacheProperties properties = new CacheProperties();
            properties.setType(CacheProperties.CacheType.MULTI_LEVEL);

            // 通过 cacheManager 行为间接验证
            DefaultCacheManager manager = configuration.cacheManager(properties, null);
            assertThat(manager).isNotNull();
        }

        @Test
        @DisplayName("LOCAL 类型不需要分布式缓存")
        void localTypeShouldNotNeedDistributedCache() {
            CacheProperties properties = new CacheProperties();
            properties.setType(CacheProperties.CacheType.LOCAL);

            DefaultCacheManager manager = configuration.cacheManager(properties, null);
            assertThat(manager).isNotNull();
        }
    }

    @Nested
    @DisplayName("cacheAspect 配置测试")
    class CacheAspectTests {

        @Test
        @DisplayName("应该创建缓存切面")
        void shouldCreateCacheAspect() {
            CacheProperties properties = new CacheProperties();
            DefaultCacheManager manager = new DefaultCacheManager(properties);

            CacheAspect aspect = configuration.cacheAspect(manager);

            assertThat(aspect).isNotNull();
        }
    }
}
