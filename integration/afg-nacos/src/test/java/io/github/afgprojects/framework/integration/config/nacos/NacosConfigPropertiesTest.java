package io.github.afgprojects.framework.integration.config.nacos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NacosConfigProperties 单元测试
 */
@DisplayName("NacosConfigProperties 测试")
class NacosConfigPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            NacosConfigProperties properties = new NacosConfigProperties();

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getServerAddr()).isEqualTo("localhost:8848");
            assertThat(properties.getNamespace()).isNull();
            assertThat(properties.getGroup()).isEqualTo("DEFAULT_GROUP");
            assertThat(properties.getUsername()).isNull();
            assertThat(properties.getPassword()).isNull();
            assertThat(properties.getAccessToken()).isNull();
            assertThat(properties.getConnectTimeout()).isEqualTo(5000);
            assertThat(properties.getReadTimeout()).isEqualTo(10000);
            assertThat(properties.getPollTimeout()).isEqualTo(30000);
            assertThat(properties.getMaxRetries()).isEqualTo(3);
            assertThat(properties.getRetryInterval()).isEqualTo(1000);
            assertThat(properties.isCacheEnabled()).isTrue();
            assertThat(properties.getCacheDir()).isNull();
            assertThat(properties.getContextPath()).isNull();
            assertThat(properties.getEndpoint()).isNull();
            assertThat(properties.isSecure()).isFalse();
        }
    }

    @Nested
    @DisplayName("属性设置测试")
    class PropertySetterTests {

        @Test
        @DisplayName("应该正确设置 enabled 属性")
        void shouldSetEnabledProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setEnabled(false);

            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置 serverAddr 属性")
        void shouldSetServerAddrProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setServerAddr("nacos.example.com:8848");

            assertThat(properties.getServerAddr()).isEqualTo("nacos.example.com:8848");
        }

        @Test
        @DisplayName("应该正确设置多节点 serverAddr 属性")
        void shouldSetMultipleServerAddrProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setServerAddr("nacos1:8848,nacos2:8848,nacos3:8848");

            assertThat(properties.getServerAddr()).isEqualTo("nacos1:8848,nacos2:8848,nacos3:8848");
        }

        @Test
        @DisplayName("应该正确设置 namespace 属性")
        void shouldSetNamespaceProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setNamespace("dev-namespace-id");

            assertThat(properties.getNamespace()).isEqualTo("dev-namespace-id");
        }

        @Test
        @DisplayName("应该正确设置 group 属性")
        void shouldSetGroupProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setGroup("CUSTOM_GROUP");

            assertThat(properties.getGroup()).isEqualTo("CUSTOM_GROUP");
        }

        @Test
        @DisplayName("应该正确设置 username 属性")
        void shouldSetUsernameProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setUsername("admin");

            assertThat(properties.getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("应该正确设置 password 属性")
        void shouldSetPasswordProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setPassword("secret123");

            assertThat(properties.getPassword()).isEqualTo("secret123");
        }

        @Test
        @DisplayName("应该正确设置 accessToken 属性")
        void shouldSetAccessTokenProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setAccessToken("token-xyz-123");

            assertThat(properties.getAccessToken()).isEqualTo("token-xyz-123");
        }

        @Test
        @DisplayName("应该正确设置 connectTimeout 属性")
        void shouldSetConnectTimeoutProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setConnectTimeout(10000);

            assertThat(properties.getConnectTimeout()).isEqualTo(10000);
        }

        @Test
        @DisplayName("应该正确设置 readTimeout 属性")
        void shouldSetReadTimeoutProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setReadTimeout(20000);

            assertThat(properties.getReadTimeout()).isEqualTo(20000);
        }

        @Test
        @DisplayName("应该正确设置 pollTimeout 属性")
        void shouldSetPollTimeoutProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setPollTimeout(60000);

            assertThat(properties.getPollTimeout()).isEqualTo(60000);
        }

        @Test
        @DisplayName("应该正确设置 maxRetries 属性")
        void shouldSetMaxRetriesProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setMaxRetries(5);

            assertThat(properties.getMaxRetries()).isEqualTo(5);
        }

        @Test
        @DisplayName("应该正确设置 retryInterval 属性")
        void shouldSetRetryIntervalProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setRetryInterval(2000);

            assertThat(properties.getRetryInterval()).isEqualTo(2000);
        }

        @Test
        @DisplayName("应该正确设置 cacheEnabled 属性")
        void shouldSetCacheEnabledProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setCacheEnabled(false);

            assertThat(properties.isCacheEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置 cacheDir 属性")
        void shouldSetCacheDirProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setCacheDir("/var/cache/nacos");

            assertThat(properties.getCacheDir()).isEqualTo("/var/cache/nacos");
        }

        @Test
        @DisplayName("应该正确设置 contextPath 属性")
        void shouldSetContextPathProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setContextPath("/nacos");

            assertThat(properties.getContextPath()).isEqualTo("/nacos");
        }

        @Test
        @DisplayName("应该正确设置 endpoint 属性")
        void shouldSetEndpointProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setEndpoint("endpoint.nacos.com");

            assertThat(properties.getEndpoint()).isEqualTo("endpoint.nacos.com");
        }

        @Test
        @DisplayName("应该正确设置 secure 属性")
        void shouldSetSecureProperty() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setSecure(true);

            assertThat(properties.isSecure()).isTrue();
        }
    }

    @Nested
    @DisplayName("多实例测试")
    class MultipleInstanceTests {

        @Test
        @DisplayName("不同的实例应该有独立的属性值")
        void shouldHaveIndependentPropertyValues() {
            NacosConfigProperties properties1 = new NacosConfigProperties();
            NacosConfigProperties properties2 = new NacosConfigProperties();

            properties1.setNamespace("namespace-1");
            properties2.setNamespace("namespace-2");

            assertThat(properties1.getNamespace()).isEqualTo("namespace-1");
            assertThat(properties2.getNamespace()).isEqualTo("namespace-2");
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("应该允许设置空字符串作为 namespace")
        void shouldAllowEmptyNamespace() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setNamespace("");

            assertThat(properties.getNamespace()).isEmpty();
        }

        @Test
        @DisplayName("应该允许设置不同的超时值")
        void shouldAllowDifferentTimeoutValues() {
            NacosConfigProperties properties = new NacosConfigProperties();

            properties.setConnectTimeout(0);
            assertThat(properties.getConnectTimeout()).isEqualTo(0);

            properties.setConnectTimeout(60000);
            assertThat(properties.getConnectTimeout()).isEqualTo(60000);
        }
    }
}
