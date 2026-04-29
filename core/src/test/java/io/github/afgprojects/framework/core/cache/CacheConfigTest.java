package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CacheConfig 测试
 */
@DisplayName("CacheConfig 测试")
class CacheConfigTest {

    @Nested
    @DisplayName("默认配置测试")
    class DefaultConfigTests {

        @Test
        @DisplayName("应该创建默认配置")
        void shouldCreateDefaultConfig() {
            // when
            CacheConfig config = CacheConfig.defaultConfig();

            // then
            assertThat(config.getDefaultTtl()).isEqualTo(0);
            assertThat(config.getMaximumSize()).isEqualTo(10000);
            assertThat(config.isCacheNull()).isTrue();
            assertThat(config.getNullValueTtl()).isEqualTo(60000);
            assertThat(config.getInitialCapacity()).isEqualTo(128);
            assertThat(config.isRecordStats()).isTrue();
        }
    }

    @Nested
    @DisplayName("构建器模式测试")
    class BuilderPatternTests {

        private CacheConfig config;

        @BeforeEach
        void setUp() {
            config = CacheConfig.defaultConfig()
                    .defaultTtl(300000)
                    .maximumSize(5000)
                    .cacheNull(false)
                    .nullValueTtl(30000)
                    .initialCapacity(256)
                    .recordStats(false);
        }

        @Test
        @DisplayName("应该正确设置默认过期时间")
        void shouldSetDefaultTtl() {
            assertThat(config.getDefaultTtl()).isEqualTo(300000);
        }

        @Test
        @DisplayName("应该正确设置最大容量")
        void shouldSetMaximumSize() {
            assertThat(config.getMaximumSize()).isEqualTo(5000);
        }

        @Test
        @DisplayName("应该正确设置是否缓存 null")
        void shouldSetCacheNull() {
            assertThat(config.isCacheNull()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置空值过期时间")
        void shouldSetNullValueTtl() {
            assertThat(config.getNullValueTtl()).isEqualTo(30000);
        }

        @Test
        @DisplayName("应该正确设置初始容量")
        void shouldSetInitialCapacity() {
            assertThat(config.getInitialCapacity()).isEqualTo(256);
        }

        @Test
        @DisplayName("应该正确设置是否开启统计")
        void shouldSetRecordStats() {
            assertThat(config.isRecordStats()).isFalse();
        }
    }
}