package io.github.afgprojects.framework.integration.config.consul;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConsulConfigProperties 单元测试
 */
@DisplayName("ConsulConfigProperties 测试")
class ConsulConfigPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            ConsulConfigProperties properties = new ConsulConfigProperties();

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getHost()).isEqualTo("localhost");
            assertThat(properties.getPort()).isEqualTo(8500);
            assertThat(properties.getPrefix()).isEqualTo("config/afg");
            assertThat(properties.getToken()).isNull();
            assertThat(properties.isCacheEnabled()).isTrue();
            assertThat(properties.getRefreshInterval()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("属性设置测试")
    class PropertySetterTests {

        @Test
        @DisplayName("应该正确设置 enabled 属性")
        void shouldSetEnabledProperty() {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            properties.setEnabled(false);

            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置 host 属性")
        void shouldSetHostProperty() {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            properties.setHost("consul.example.com");

            assertThat(properties.getHost()).isEqualTo("consul.example.com");
        }

        @Test
        @DisplayName("应该正确设置 port 属性")
        void shouldSetPortProperty() {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            properties.setPort(9500);

            assertThat(properties.getPort()).isEqualTo(9500);
        }

        @Test
        @DisplayName("应该正确设置 prefix 属性")
        void shouldSetPrefixProperty() {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            properties.setPrefix("config/myapp");

            assertThat(properties.getPrefix()).isEqualTo("config/myapp");
        }

        @Test
        @DisplayName("应该正确设置 token 属性")
        void shouldSetTokenProperty() {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            properties.setToken("my-acl-token");

            assertThat(properties.getToken()).isEqualTo("my-acl-token");
        }

        @Test
        @DisplayName("应该正确设置 cacheEnabled 属性")
        void shouldSetCacheEnabledProperty() {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            properties.setCacheEnabled(false);

            assertThat(properties.isCacheEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置 refreshInterval 属性")
        void shouldSetRefreshIntervalProperty() {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            properties.setRefreshInterval(10);

            assertThat(properties.getRefreshInterval()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("多实例测试")
    class MultipleInstanceTests {

        @Test
        @DisplayName("不同的实例应该有独立的属性值")
        void shouldHaveIndependentPropertyValues() {
            ConsulConfigProperties properties1 = new ConsulConfigProperties();
            ConsulConfigProperties properties2 = new ConsulConfigProperties();

            properties1.setHost("host1.example.com");
            properties2.setHost("host2.example.com");

            assertThat(properties1.getHost()).isEqualTo("host1.example.com");
            assertThat(properties2.getHost()).isEqualTo("host2.example.com");
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("应该允许设置空字符串作为 token")
        void shouldAllowEmptyToken() {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            properties.setToken("");

            assertThat(properties.getToken()).isEmpty();
        }

        @Test
        @DisplayName("应该允许设置不同的端口值")
        void shouldAllowDifferentPortValues() {
            ConsulConfigProperties properties = new ConsulConfigProperties();

            properties.setPort(80);
            assertThat(properties.getPort()).isEqualTo(80);

            properties.setPort(443);
            assertThat(properties.getPort()).isEqualTo(443);

            properties.setPort(8500);
            assertThat(properties.getPort()).isEqualTo(8500);
        }
    }
}
