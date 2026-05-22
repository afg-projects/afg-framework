package io.github.afgprojects.framework.core.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * HttpClientProperties 单元测试。
 * <p>
 * 测试 HTTP 客户端配置属性类及其嵌套配置类的默认值和属性设置。
 *
 * @see AfgCoreProperties.HttpClientConfig
 */
@DisplayName("HttpClientProperties 测试")
class HttpClientPropertiesTest {

    /**
     * 测试默认值配置。
     */
    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        /**
         * 测试配置类有正确的默认值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            AfgCoreProperties.HttpClientConfig props = new AfgCoreProperties.HttpClientConfig();

            assertThat(props.getConnectTimeout()).isEqualTo(5000);
            assertThat(props.getReadTimeout()).isEqualTo(30000);
            assertThat(props.getRetry()).isNotNull();
            assertThat(props.getCircuitBreaker()).isNotNull();
        }
    }

    /**
     * 测试重试配置类。
     */
    @Nested
    @DisplayName("HttpRetryConfig 测试")
    class RetryConfigTests {

        /**
         * 测试重试配置类有正确的默认值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            AfgCoreProperties.HttpClientConfig.HttpRetryConfig config = new AfgCoreProperties.HttpClientConfig.HttpRetryConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getMaxAttempts()).isEqualTo(3);
            assertThat(config.getInitialInterval()).isEqualTo(1000);
            assertThat(config.getMultiplier()).isEqualTo(2.0);
            assertThat(config.getMaxInterval()).isEqualTo(10000);
            assertThat(config.getRetryOnStatus()).containsExactlyInAnyOrder(502, 503, 504);
        }

        /**
         * 测试正确设置重试配置属性。
         */
        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            AfgCoreProperties.HttpClientConfig.HttpRetryConfig config = new AfgCoreProperties.HttpClientConfig.HttpRetryConfig();
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

    /**
     * 测试熔断配置类。
     */
    @Nested
    @DisplayName("HttpCircuitBreakerConfig 测试")
    class CircuitBreakerConfigTests {

        /**
         * 测试熔断配置类有正确的默认值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            AfgCoreProperties.HttpClientConfig.HttpCircuitBreakerConfig config = new AfgCoreProperties.HttpClientConfig.HttpCircuitBreakerConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getFailureThreshold()).isEqualTo(5);
            assertThat(config.getOpenDuration()).isEqualTo(30000);
            assertThat(config.getHalfOpenMaxCalls()).isEqualTo(3);
            assertThat(config.getSuccessThreshold()).isEqualTo(3);
        }

        /**
         * 测试正确设置熔断配置属性。
         */
        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            AfgCoreProperties.HttpClientConfig.HttpCircuitBreakerConfig config = new AfgCoreProperties.HttpClientConfig.HttpCircuitBreakerConfig();
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
}
