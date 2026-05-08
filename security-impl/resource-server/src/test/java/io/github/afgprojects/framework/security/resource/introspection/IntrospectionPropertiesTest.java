package io.github.afgprojects.framework.security.resource.introspection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * IntrospectionProperties 测试类。
 *
 * @since 1.0.0
 */
class IntrospectionPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("enabled 默认值为 false")
        void shouldDefaultEnabledToFalse() {
            IntrospectionProperties properties = new IntrospectionProperties();
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("cacheTtl 默认值为 1 分钟")
        void shouldDefaultCacheTtlTo1Minute() {
            IntrospectionProperties properties = new IntrospectionProperties();
            assertThat(properties.getCacheTtl()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("connectTimeout 默认值为 5 秒")
        void shouldDefaultConnectTimeoutTo5Seconds() {
            IntrospectionProperties properties = new IntrospectionProperties();
            assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("readTimeout 默认值为 10 秒")
        void shouldDefaultReadTimeoutTo10Seconds() {
            IntrospectionProperties properties = new IntrospectionProperties();
            assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("verifyActive 默认值为 true")
        void shouldDefaultVerifyActiveToTrue() {
            IntrospectionProperties properties = new IntrospectionProperties();
            assertThat(properties.isVerifyActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("setter 测试")
    class SetterTests {

        @Test
        @DisplayName("设置 enabled")
        void shouldSetEnabled() {
            IntrospectionProperties properties = new IntrospectionProperties();
            properties.setEnabled(true);
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("设置 introspectionUri")
        void shouldSetIntrospectionUri() {
            IntrospectionProperties properties = new IntrospectionProperties();
            properties.setIntrospectionUri("https://auth.example.com/oauth2/introspect");
            assertThat(properties.getIntrospectionUri()).isEqualTo("https://auth.example.com/oauth2/introspect");
        }

        @Test
        @DisplayName("设置 clientId")
        void shouldSetClientId() {
            IntrospectionProperties properties = new IntrospectionProperties();
            properties.setClientId("resource-server");
            assertThat(properties.getClientId()).isEqualTo("resource-server");
        }

        @Test
        @DisplayName("设置 clientSecret")
        void shouldSetClientSecret() {
            IntrospectionProperties properties = new IntrospectionProperties();
            properties.setClientSecret("secret123");
            assertThat(properties.getClientSecret()).isEqualTo("secret123");
        }

        @Test
        @DisplayName("设置 cacheTtl")
        void shouldSetCacheTtl() {
            IntrospectionProperties properties = new IntrospectionProperties();
            properties.setCacheTtl(Duration.ofMinutes(5));
            assertThat(properties.getCacheTtl()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("设置 connectTimeout")
        void shouldSetConnectTimeout() {
            IntrospectionProperties properties = new IntrospectionProperties();
            properties.setConnectTimeout(Duration.ofSeconds(10));
            assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("设置 readTimeout")
        void shouldSetReadTimeout() {
            IntrospectionProperties properties = new IntrospectionProperties();
            properties.setReadTimeout(Duration.ofSeconds(30));
            assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("设置 verifyActive")
        void shouldSetVerifyActive() {
            IntrospectionProperties properties = new IntrospectionProperties();
            properties.setVerifyActive(false);
            assertThat(properties.isVerifyActive()).isFalse();
        }
    }
}