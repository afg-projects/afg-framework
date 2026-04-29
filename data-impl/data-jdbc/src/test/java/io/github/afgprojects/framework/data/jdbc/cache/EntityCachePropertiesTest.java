package io.github.afgprojects.framework.data.jdbc.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EntityCacheProperties 单元测试
 */
@DisplayName("EntityCacheProperties 测试")
class EntityCachePropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            EntityCacheProperties properties = new EntityCacheProperties();

            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getTtl()).isEqualTo(0);
            assertThat(properties.getMaxSize()).isEqualTo(10000);
            assertThat(properties.isCacheNull()).isTrue();
            assertThat(properties.getNullValueTtl()).isEqualTo(60000);
        }
    }

    @Nested
    @DisplayName("Setter 测试")
    class SetterTests {

        @Test
        @DisplayName("应该正确设置 enabled 属性")
        void shouldSetEnabledProperty() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);

            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("应该正确设置 ttl 属性")
        void shouldSetTtlProperty() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setTtl(3600000);

            assertThat(properties.getTtl()).isEqualTo(3600000);
        }

        @Test
        @DisplayName("应该正确设置 maxSize 属性")
        void shouldSetMaxSizeProperty() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setMaxSize(5000);

            assertThat(properties.getMaxSize()).isEqualTo(5000);
        }

        @Test
        @DisplayName("应该正确设置 cacheNull 属性")
        void shouldSetCacheNullProperty() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setCacheNull(false);

            assertThat(properties.isCacheNull()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置 nullValueTtl 属性")
        void shouldSetNullValueTtlProperty() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setNullValueTtl(30000);

            assertThat(properties.getNullValueTtl()).isEqualTo(30000);
        }
    }

    @Nested
    @DisplayName("配置场景测试")
    class ConfigurationScenarioTests {

        @Test
        @DisplayName("应该支持生产环境配置")
        void shouldSupportProductionConfiguration() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            properties.setTtl(3600000); // 1 hour
            properties.setMaxSize(100000);
            properties.setCacheNull(true);
            properties.setNullValueTtl(60000); // 1 minute

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getTtl()).isEqualTo(3600000);
            assertThat(properties.getMaxSize()).isEqualTo(100000);
            assertThat(properties.isCacheNull()).isTrue();
            assertThat(properties.getNullValueTtl()).isEqualTo(60000);
        }

        @Test
        @DisplayName("应该支持开发环境配置")
        void shouldSupportDevelopmentConfiguration() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(false); // No cache in dev

            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该支持短 TTL 配置")
        void shouldSupportShortTtlConfiguration() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            properties.setTtl(60000); // 1 minute TTL

            assertThat(properties.getTtl()).isEqualTo(60000);
        }

        @Test
        @DisplayName("应该支持永不过期配置")
        void shouldSupportNeverExpireConfiguration() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            properties.setTtl(0); // Never expire

            assertThat(properties.getTtl()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("多实例测试")
    class MultipleInstanceTests {

        @Test
        @DisplayName("不同的实例应该有独立的属性值")
        void shouldHaveIndependentPropertyValues() {
            EntityCacheProperties properties1 = new EntityCacheProperties();
            EntityCacheProperties properties2 = new EntityCacheProperties();

            properties1.setTtl(1000);
            properties2.setTtl(2000);

            assertThat(properties1.getTtl()).isEqualTo(1000);
            assertThat(properties2.getTtl()).isEqualTo(2000);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("应该允许设置 TTL 为 0")
        void shouldAllowZeroTtl() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setTtl(0);

            assertThat(properties.getTtl()).isEqualTo(0);
        }

        @Test
        @DisplayName("应该允许设置大 TTL")
        void shouldAllowLargeTtl() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setTtl(86400000L); // 24 hours

            assertThat(properties.getTtl()).isEqualTo(86400000L);
        }

        @Test
        @DisplayName("应该允许设置 maxSize 为 0")
        void shouldAllowZeroMaxSize() {
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setMaxSize(0);

            assertThat(properties.getMaxSize()).isEqualTo(0);
        }
    }
}