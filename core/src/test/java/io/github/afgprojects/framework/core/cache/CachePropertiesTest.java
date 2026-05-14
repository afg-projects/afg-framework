package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CacheProperties 单元测试。
 * <p>
 * 测试缓存属性配置的功能，包括默认值、本地缓存配置、分布式缓存配置、配置转换和命名缓存配置。
 * </p>
 *
 * @see CacheProperties
 */
@DisplayName("CacheProperties 测试")
class CachePropertiesTest {

    /**
     * 默认值测试。
     * <p>
     * 测试缓存属性的默认配置值。
     * </p>
     */
    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        /**
         * 测试默认启用缓存。
         */
        @Test
        @DisplayName("默认应该启用缓存")
        void shouldBeEnabledByDefault() {
            CacheProperties properties = new CacheProperties();
            assertThat(properties.isEnabled()).isTrue();
        }

        /**
         * 测试默认缓存类型为 LOCAL。
         */
        @Test
        @DisplayName("默认缓存类型应该是 LOCAL")
        void shouldDefaultToLocalCacheType() {
            CacheProperties properties = new CacheProperties();
            assertThat(properties.getType()).isEqualTo(CacheProperties.CacheType.LOCAL);
        }

        /**
         * 测试默认缓存 null 值。
         */
        @Test
        @DisplayName("默认应该缓存 null 值")
        void shouldCacheNullByDefault() {
            CacheProperties properties = new CacheProperties();
            assertThat(properties.isCacheNull()).isTrue();
        }
    }

    /**
     * 本地缓存配置测试。
     * <p>
     * 测试本地缓存配置的设置。
     * </p>
     */
    @Nested
    @DisplayName("本地缓存配置测试")
    class LocalConfigTests {

        /**
         * 测试正确设置本地缓存配置。
         */
        @Test
        @DisplayName("应该正确设置本地缓存配置")
        void shouldSetLocalConfig() {
            CacheProperties properties = new CacheProperties();
            properties.getLocal().setMaximumSize(5000);
            properties.getLocal().setInitialCapacity(256);
            properties.getLocal().setExpireAfterWrite(Duration.ofMinutes(5));

            assertThat(properties.getLocal().getMaximumSize()).isEqualTo(5000);
            assertThat(properties.getLocal().getInitialCapacity()).isEqualTo(256);
            assertThat(properties.getLocal().getExpireAfterWrite()).isEqualTo(Duration.ofMinutes(5));
        }
    }

    /**
     * 分布式缓存配置测试。
     * <p>
     * 测试分布式缓存配置的设置。
     * </p>
     */
    @Nested
    @DisplayName("分布式缓存配置测试")
    class DistributedConfigTests {

        /**
         * 测试正确设置分布式缓存配置。
         */
        @Test
        @DisplayName("应该正确设置分布式缓存配置")
        void shouldSetDistributedConfig() {
            CacheProperties properties = new CacheProperties();
            properties.getDistributed().setEnabled(true);
            properties.getDistributed().setKeyPrefix("my-cache:");
            properties.getDistributed().setDefaultTtl(3600000);

            assertThat(properties.getDistributed().isEnabled()).isTrue();
            assertThat(properties.getDistributed().getKeyPrefix()).isEqualTo("my-cache:");
            assertThat(properties.getDistributed().getDefaultTtl()).isEqualTo(3600000);
        }
    }

    /**
     * toCacheConfig 测试。
     * <p>
     * 测试属性转换为 CacheConfig。
     * </p>
     */
    @Nested
    @DisplayName("toCacheConfig 测试")
    class ToCacheConfigTests {

        /**
         * 测试正确转换为 CacheConfig。
         */
        @Test
        @DisplayName("应该正确转换为 CacheConfig")
        void shouldConvertToCacheConfig() {
            CacheProperties properties = new CacheProperties();
            properties.setDefaultTtl(60000);
            properties.setCacheNull(false);
            properties.getLocal().setMaximumSize(5000);

            CacheConfig config = properties.toCacheConfig();

            assertThat(config.getDefaultTtl()).isEqualTo(60000);
            assertThat(config.isCacheNull()).isFalse();
            assertThat(config.getMaximumSize()).isEqualTo(5000);
        }
    }

    /**
     * 命名缓存配置测试。
     * <p>
     * 测试获取命名缓存配置的功能。
     * </p>
     */
    @Nested
    @DisplayName("命名缓存配置测试")
    class NamedCacheConfigTests {

        /**
         * 测试正确获取命名缓存配置。
         */
        @Test
        @DisplayName("应该正确获取命名缓存配置")
        void shouldGetNamedCacheConfig() {
            CacheProperties properties = new CacheProperties();
            CacheConfig customConfig = CacheConfig.defaultConfig()
                    .maximumSize(100)
                    .defaultTtl(30000);
            properties.getCaches().put("users", customConfig);

            CacheConfig result = properties.getCacheConfig("users");

            assertThat(result.getMaximumSize()).isEqualTo(100);
            assertThat(result.getDefaultTtl()).isEqualTo(30000);
        }

        /**
         * 测试未配置的缓存使用默认配置。
         */
        @Test
        @DisplayName("未配置的缓存应该使用默认配置")
        void shouldUseDefaultConfigForUnconfiguredCache() {
            CacheProperties properties = new CacheProperties();
            properties.setDefaultTtl(60000);

            CacheConfig result = properties.getCacheConfig("unknown-cache");

            assertThat(result.getDefaultTtl()).isEqualTo(60000);
        }
    }

    /**
     * CacheType 枚举测试。
     * <p>
     * 测试缓存类型枚举的完整性。
     * </p>
     */
    @Nested
    @DisplayName("CacheType 枚举测试")
    class CacheTypeTests {

        /**
         * 测试包含所有缓存类型。
         */
        @Test
        @DisplayName("应该包含所有缓存类型")
        void shouldContainAllCacheTypes() {
            CacheProperties.CacheType[] types = CacheProperties.CacheType.values();

            assertThat(types).containsExactly(
                    CacheProperties.CacheType.LOCAL,
                    CacheProperties.CacheType.DISTRIBUTED,
                    CacheProperties.CacheType.MULTI_LEVEL
            );
        }
    }
}