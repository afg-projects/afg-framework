package io.github.afgprojects.framework.core.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * HttpClientProperties 测试
 */
@DisplayName("HttpClientProperties 测试")
class HttpClientPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            HttpClientProperties props = new HttpClientProperties();

            assertThat(props.getConnectTimeout()).isEqualTo(5000);
            assertThat(props.getReadTimeout()).isEqualTo(30000);
            assertThat(props.getRetry()).isNotNull();
            assertThat(props.getCircuitBreaker()).isNotNull();
        }
    }

    @Nested
    @DisplayName("RetryConfig 测试")
    class RetryConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            HttpClientProperties.RetryConfig config = new HttpClientProperties.RetryConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getMaxAttempts()).isEqualTo(3);
            assertThat(config.getInitialInterval()).isEqualTo(1000);
            assertThat(config.getMultiplier()).isEqualTo(2.0);
            assertThat(config.getMaxInterval()).isEqualTo(10000);
            assertThat(config.getRetryOnStatus()).containsExactlyInAnyOrder(502, 503, 504);
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            HttpClientProperties.RetryConfig config = new HttpClientProperties.RetryConfig();
            config.setEnabled(false);
            config.setMaxAttempts(5);
            config.setInitialInterval(2000);
            config.setMultiplier(3.0);
            config.setMaxInterval(20000);
            config.setRetryOnStatus(Set.of(500, 502));

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getMaxAttempts()).isEqualTo(5);
            assertThat(config.getInitialInterval()).isEqualTo(2000);
            assertThat(config.getMultiplier()).isEqualTo(3.0);
            assertThat(config.getMaxInterval()).isEqualTo(20000);
            assertThat(config.getRetryOnStatus()).containsExactlyInAnyOrder(500, 502);
        }
    }

    @Nested
    @DisplayName("CircuitBreakerConfig 测试")
    class CircuitBreakerConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            HttpClientProperties.CircuitBreakerConfig config = new HttpClientProperties.CircuitBreakerConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getFailureThreshold()).isEqualTo(5);
            assertThat(config.getOpenDuration()).isEqualTo(30000);
            assertThat(config.getHalfOpenMaxCalls()).isEqualTo(3);
            assertThat(config.getSuccessThreshold()).isEqualTo(3);
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            HttpClientProperties.CircuitBreakerConfig config = new HttpClientProperties.CircuitBreakerConfig();
            config.setEnabled(false);
            config.setFailureThreshold(10);
            config.setOpenDuration(60000);
            config.setHalfOpenMaxCalls(5);
            config.setSuccessThreshold(5);

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getFailureThreshold()).isEqualTo(10);
            assertThat(config.getOpenDuration()).isEqualTo(60000);
            assertThat(config.getHalfOpenMaxCalls()).isEqualTo(5);
            assertThat(config.getSuccessThreshold()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("NamedClientConfig 测试")
    class NamedClientConfigTests {

        @Test
        @DisplayName("应该正确合并配置")
        void shouldMergeConfig() {
            HttpClientProperties defaults = new HttpClientProperties();
            defaults.setConnectTimeout(5000);
            defaults.setReadTimeout(30000);

            HttpClientProperties.NamedClientConfig namedConfig = new HttpClientProperties.NamedClientConfig();
            namedConfig.setBaseUrl("http://example.com");
            namedConfig.setConnectTimeout(10000);
            namedConfig.setReadTimeout(0); // 使用默认值

            HttpClientProperties merged = namedConfig.merge(defaults);

            assertThat(merged.getConnectTimeout()).isEqualTo(10000);
            assertThat(merged.getReadTimeout()).isEqualTo(30000);
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            HttpClientProperties.NamedClientConfig config = new HttpClientProperties.NamedClientConfig();
            config.setBaseUrl("http://example.com");
            config.setConnectTimeout(10000);
            config.setReadTimeout(60000);

            assertThat(config.getBaseUrl()).isEqualTo("http://example.com");
            assertThat(config.getConnectTimeout()).isEqualTo(10000);
            assertThat(config.getReadTimeout()).isEqualTo(60000);
        }
    }
}
