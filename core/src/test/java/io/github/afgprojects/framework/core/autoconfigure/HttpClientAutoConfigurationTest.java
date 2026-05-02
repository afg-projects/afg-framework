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
import io.github.afgprojects.framework.core.client.HttpClientProperties;
import io.github.afgprojects.framework.core.client.HttpClientRegistry;
import io.micrometer.tracing.Tracer;

/**
 * HttpClientAutoConfiguration 测试
 */
@DisplayName("HttpClientAutoConfiguration 测试")
class HttpClientAutoConfigurationTest {

    private HttpClientAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new HttpClientAutoConfiguration();
    }

    @Nested
    @DisplayName("restClientBuilder 配置测试")
    class RestClientBuilderTests {

        @Test
        @DisplayName("应该创建 RestClient Builder")
        void shouldCreateRestClientBuilder() {
            HttpClientProperties properties = new HttpClientProperties();

            var builder = configuration.restClientBuilder(properties);

            assertThat(builder).isNotNull();
        }

        @Test
        @DisplayName("启用重试时应该添加 ResilienceInterceptor")
        void shouldAddResilienceInterceptorWhenRetryEnabled() {
            HttpClientProperties properties = new HttpClientProperties();
            properties.getRetry().setEnabled(true);

            var builder = configuration.restClientBuilder(properties);

            assertThat(builder).isNotNull();
        }

        @Test
        @DisplayName("启用熔断时应该添加 ResilienceInterceptor")
        void shouldAddResilienceInterceptorWhenCircuitBreakerEnabled() {
            HttpClientProperties properties = new HttpClientProperties();
            properties.getCircuitBreaker().setEnabled(true);

            var builder = configuration.restClientBuilder(properties);

            assertThat(builder).isNotNull();
        }
    }

    @Nested
    @DisplayName("resilienceScheduler 配置测试")
    class ResilienceSchedulerTests {

        @Test
        @DisplayName("应该创建调度器")
        void shouldCreateScheduler() {
            ScheduledExecutorService scheduler = configuration.resilienceScheduler();

            assertThat(scheduler).isNotNull();
            scheduler.shutdown();
        }
    }

    @Nested
    @DisplayName("asyncResilienceInterceptor 配置测试")
    class AsyncResilienceInterceptorTests {

        @Test
        @DisplayName("应该创建异步弹性拦截器")
        void shouldCreateAsyncResilienceInterceptor() {
            HttpClientProperties properties = new HttpClientProperties();
            ScheduledExecutorService scheduler = configuration.resilienceScheduler();

            AsyncResilienceInterceptor interceptor = configuration.asyncResilienceInterceptor(
                    properties, scheduler);

            assertThat(interceptor).isNotNull();
            scheduler.shutdown();
        }
    }

    @Nested
    @DisplayName("httpClientRegistry 配置测试")
    class HttpClientRegistryTests {

        @Test
        @DisplayName("应该创建 HTTP 客户端注册表")
        void shouldCreateHttpClientRegistry() {
            Environment environment = mock(Environment.class);
            HttpClientProperties properties = new HttpClientProperties();
            var builder = configuration.restClientBuilder(properties);

            HttpClientRegistry registry = configuration.httpClientRegistry(
                    environment, builder, properties);

            assertThat(registry).isNotNull();
        }
    }
}
