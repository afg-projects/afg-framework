package io.github.afgprojects.framework.core.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClient;

import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * HttpClientAutoConfiguration 集成测试。
 * <p>
 * 测试 HTTP 客户端自动配置功能，包括属性绑定、Bean 创建等。
 *
 * @see HttpClientAutoConfiguration
 * @see HttpClientProperties
 */
@DisplayName("HttpClientAutoConfiguration 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.http-client.enabled=true",
                "afg.http-client.connect-timeout=5000",
                "afg.http-client.read-timeout=30000",
                "afg.http-client.retry.enabled=true",
                "afg.http-client.retry.max-attempts=3",
                "afg.http-client.retry.initial-interval=1000",
                "afg.http-client.circuit-breaker.enabled=true",
                "afg.http-client.circuit-breaker.failure-threshold=5",
                "afg.http-client.circuit-breaker.open-duration=30000"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HttpClientAutoConfigurationIntegrationTest {

    @Autowired(required = false)
    private HttpClientProperties httpClientProperties;

    @Autowired(required = false)
    private RestClient.Builder restClientBuilder;

    @Autowired(required = false)
    private HttpClientRegistry httpClientRegistry;

    @Autowired(required = false)
    private AsyncResilienceInterceptor asyncResilienceInterceptor;

    /**
     * 测试 HTTP 客户端 Bean 自动配置。
     */
    @Nested
    @DisplayName("HTTP 客户端配置测试")
    class HttpClientConfigTests {

        /**
         * 测试自动配置 HTTP 客户端属性 Bean。
         */
        @Test
        @DisplayName("应该自动配置 HTTP 客户端属性")
        void shouldAutoConfigureHttpClientProperties() {
            assertThat(httpClientProperties).isNotNull();
        }

        /**
         * 测试自动配置 RestClient Builder Bean。
         */
        @Test
        @DisplayName("应该自动配置 RestClient Builder")
        void shouldAutoConfigureRestClientBuilder() {
            assertThat(restClientBuilder).isNotNull();
        }

        /**
         * 测试自动配置 HttpClientRegistry Bean。
         */
        @Test
        @DisplayName("应该自动配置 HttpClientRegistry")
        void shouldAutoConfigureHttpClientRegistry() {
            assertThat(httpClientRegistry).isNotNull();
        }

        /**
         * 测试自动配置 AsyncResilienceInterceptor Bean。
         */
        @Test
        @DisplayName("应该自动配置 AsyncResilienceInterceptor")
        void shouldAutoConfigureAsyncResilienceInterceptor() {
            assertThat(asyncResilienceInterceptor).isNotNull();
        }
    }

    /**
     * 测试连接配置属性绑定。
     */
    @Nested
    @DisplayName("连接配置测试")
    class ConnectionConfigTests {

        /**
         * 测试正确配置连接超时。
         */
        @Test
        @DisplayName("应该正确配置连接超时")
        void shouldConfigureConnectTimeout() {
            assertThat(httpClientProperties.getConnectTimeout()).isEqualTo(5000);
        }

        /**
         * 测试正确配置读取超时。
         */
        @Test
        @DisplayName("应该正确配置读取超时")
        void shouldConfigureReadTimeout() {
            assertThat(httpClientProperties.getReadTimeout()).isEqualTo(30000);
        }
    }

    /**
     * 测试重试配置属性绑定。
     */
    @Nested
    @DisplayName("重试配置测试")
    class RetryConfigTests {

        /**
         * 测试正确配置重试启用状态。
         */
        @Test
        @DisplayName("应该正确配置重试启用状态")
        void shouldConfigureRetryEnabled() {
            assertThat(httpClientProperties.getRetry().isEnabled()).isTrue();
        }

        /**
         * 测试正确配置最大重试次数。
         */
        @Test
        @DisplayName("应该正确配置最大重试次数")
        void shouldConfigureMaxAttempts() {
            assertThat(httpClientProperties.getRetry().getMaxAttempts()).isEqualTo(3);
        }

        /**
         * 测试正确配置初始间隔。
         */
        @Test
        @DisplayName("应该正确配置初始间隔")
        void shouldConfigureInitialInterval() {
            assertThat(httpClientProperties.getRetry().getInitialInterval()).isEqualTo(1000);
        }
    }

    /**
     * 测试熔断配置属性绑定。
     */
    @Nested
    @DisplayName("熔断配置测试")
    class CircuitBreakerConfigTests {

        /**
         * 测试正确配置熔断启用状态。
         */
        @Test
        @DisplayName("应该正确配置熔断启用状态")
        void shouldConfigureCircuitBreakerEnabled() {
            assertThat(httpClientProperties.getCircuitBreaker().isEnabled()).isTrue();
        }

        /**
         * 测试正确配置失败次数阈值。
         */
        @Test
        @DisplayName("应该正确配置失败次数阈值")
        void shouldConfigureFailureThreshold() {
            assertThat(httpClientProperties.getCircuitBreaker().getFailureThreshold()).isEqualTo(5);
        }

        /**
         * 测试正确配置开启持续时间。
         */
        @Test
        @DisplayName("应该正确配置开启持续时间")
        void shouldConfigureOpenDuration() {
            assertThat(httpClientProperties.getCircuitBreaker().getOpenDuration()).isEqualTo(30000);
        }
    }

    /**
     * 测试 RestClient 功能。
     */
    @Nested
    @DisplayName("RestClient 功能测试")
    class RestClientTests {

        /**
         * 测试能够创建 RestClient。
         */
        @Test
        @DisplayName("应该能够创建 RestClient")
        void shouldCreateRestClient() {
            RestClient client = restClientBuilder.build();

            assertThat(client).isNotNull();
        }
    }
}
