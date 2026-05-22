package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CacheProperties 单元测试。
 * <p>
 * 测试缓存属性配置的功能，包括默认值、本地缓存配置、分布式缓存配置、配置转换和命名缓存配置。
 * </p>
 *
 * @see AfgCoreProperties.CacheConfig
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
            AfgCoreProperties.CacheConfig properties = new AfgCoreProperties.CacheConfig();
            assertThat(properties.isEnabled()).isTrue();
        }

        /**
         * 测试默认缓存类型为 LOCAL。
         */
        @Test
        @DisplayName("默认缓存类型应该是 LOCAL")
        void shouldDefaultToLocalCacheType() {
            AfgCoreProperties.CacheConfig properties = new AfgCoreProperties.CacheConfig();
            assertThat(properties.getType()).isEqualTo(AfgCoreProperties.CacheConfig.CacheType.LOCAL);
        }

        /**
         * 测试默认缓存 null 值。
         */
        @Test
        @DisplayName("默认应该缓存 null 值")
        void shouldCacheNullByDefault() {
            AfgCoreProperties.CacheConfig properties = new AfgCoreProperties.CacheConfig();
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
            AfgCoreProperties.CacheConfig properties = new AfgCoreProperties.CacheConfig();
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
            AfgCoreProperties.CacheConfig properties = new AfgCoreProperties.CacheConfig();
            properties.getDistributed().setEnabled(true);
            properties.getDistributed().setKeyPrefix("my-cache:");
            properties.getDistributed().setDefaultTtl(3600000);

            assertThat(properties.getDistributed().isEnabled()).isTrue();
            assertThat(properties.getDistributed().getKeyPrefix()).isEqualTo("my-cache:");
            assertThat(properties.getDistributed().getDefaultTtl()).isEqualTo(3600000);
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
            AfgCoreProperties.CacheConfig.CacheType[] types = AfgCoreProperties.CacheConfig.CacheType.values();

            assertThat(types).containsExactly(
                    AfgCoreProperties.CacheConfig.CacheType.LOCAL,
                    AfgCoreProperties.CacheConfig.CacheType.DISTRIBUTED,
                    AfgCoreProperties.CacheConfig.CacheType.MULTI_LEVEL
            );
        }
    }
}