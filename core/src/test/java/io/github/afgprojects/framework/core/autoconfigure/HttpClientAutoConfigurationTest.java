package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import io.github.afgprojects.framework.core.client.AsyncResilienceInterceptor;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.client.HttpClientRegistry;
import io.micrometer.tracing.Tracer;

/**
 * HttpClientAutoConfiguration 单元测试。
 * 测试 HTTP 客户端自动配置类的 Bean 创建功能。
 *
 * @see HttpClientAutoConfiguration
 */
@DisplayName("HttpClientAutoConfiguration 测试")
class HttpClientAutoConfigurationTest {

    private HttpClientAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new HttpClientAutoConfiguration();
    }

    /**
     * RestClient Builder 配置测试。
     * 验证 restClientBuilder Bean 的创建和拦截器配置。
     */
    @Nested
    @DisplayName("restClientBuilder 配置测试")
    class RestClientBuilderTests {

        /**
         * 测试创建 RestClient Builder。
         */
        @Test
        @DisplayName("应该创建 RestClient Builder")
        void shouldCreateRestClientBuilder() {
            AfgCoreProperties properties = new AfgCoreProperties();

            var builder = configuration.restClientBuilder(properties);

            assertThat(builder).isNotNull();
        }

        /**
         * 测试启用重试时添加 ResilienceInterceptor。
         */
        @Test
        @DisplayName("启用重试时应该添加 ResilienceInterceptor")
        void shouldAddResilienceInterceptorWhenRetryEnabled() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getHttpClient().getRetry().setEnabled(true);

            var builder = configuration.restClientBuilder(properties);

            assertThat(builder).isNotNull();
        }

        /**
         * 测试启用熔断时添加 ResilienceInterceptor。
         */
        @Test
        @DisplayName("启用熔断时应该添加 ResilienceInterceptor")
        void shouldAddResilienceInterceptorWhenCircuitBreakerEnabled() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getHttpClient().getCircuitBreaker().setEnabled(true);

            var builder = configuration.restClientBuilder(properties);

            assertThat(builder).isNotNull();
        }
    }

    /**
     * ResilienceScheduler 配置测试。
     * 验证弹性调度器的创建。
     */
    @Nested
    @DisplayName("resilienceScheduler 配置测试")
    class ResilienceSchedulerTests {

        /**
         * 测试创建调度器。
         */
        @Test
        @DisplayName("应该创建调度器")
        void shouldCreateScheduler() {
            ScheduledExecutorService scheduler = configuration.resilienceScheduler();

            assertThat(scheduler).isNotNull();
            scheduler.shutdown();
        }
    }

    /**
     * AsyncResilienceInterceptor 配置测试。
     * 验证异步弹性拦截器的创建。
     */
    @Nested
    @DisplayName("asyncResilienceInterceptor 配置测试")
    class AsyncResilienceInterceptorTests {

        /**
         * 测试创建异步弹性拦截器。
         */
        @Test
        @DisplayName("应该创建异步弹性拦截器")
        void shouldCreateAsyncResilienceInterceptor() {
            AfgCoreProperties properties = new AfgCoreProperties();
            ScheduledExecutorService scheduler = configuration.resilienceScheduler();

            AsyncResilienceInterceptor interceptor = configuration.asyncResilienceInterceptor(
                    properties, scheduler);

            assertThat(interceptor).isNotNull();
            scheduler.shutdown();
        }
    }

    /**
     * HttpClientRegistry 配置测试。
     * 验证 HTTP 客户端注册表的创建。
     */
    @Nested
    @DisplayName("httpClientRegistry 配置测试")
    class HttpClientRegistryTests {

        /**
         * 测试创建 HTTP 客户端注册表。
         */
        @Test
        @DisplayName("应该创建 HTTP 客户端注册表")
        void shouldCreateHttpClientRegistry() {
            Environment environment = mock(Environment.class);
            AfgCoreProperties properties = new AfgCoreProperties();
            var builder = configuration.restClientBuilder(properties);

            HttpClientRegistry registry = configuration.httpClientRegistry(
                    environment, builder, properties);

            assertThat(registry).isNotNull();
        }
    }
}
