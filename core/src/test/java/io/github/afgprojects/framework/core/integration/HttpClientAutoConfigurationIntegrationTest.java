package io.github.afgprojects.framework.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import io.github.afgprojects.framework.core.autoconfigure.HttpClientAutoConfiguration;
import io.github.afgprojects.framework.core.client.HttpClientRegistry;
import io.github.afgprojects.framework.core.support.BaseIntegrationTest;

/**
 * HttpClientAutoConfiguration 集成测试
 * 验证 HTTP 客户端自动配置是否正确注入所有 Bean
 */
class HttpClientAutoConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("应该成功注入 RestClient.Builder Bean")
    void shouldInjectRestClientBuilder() {
        // when
        RestClient.Builder builder = getBean(RestClient.Builder.class);

        // then
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 HttpClientRegistry Bean")
    void shouldInjectHttpClientRegistry() {
        // when
        HttpClientRegistry registry = getBean(HttpClientRegistry.class);

        // then
        assertThat(registry).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 HttpClientAutoConfiguration Bean")
    void shouldInjectHttpClientAutoConfiguration() {
        // when
        HttpClientAutoConfiguration autoConfig = getBean(HttpClientAutoConfiguration.class);

        // then
        assertThat(autoConfig).isNotNull();
    }

    @Test
    @DisplayName("HttpClientRegistry 应该能够创建 HTTP 客户端")
    void shouldCreateHttpClient() {
        // given
        HttpClientRegistry registry = getBean(HttpClientRegistry.class);

        // when
        TestClient client = registry.createClient(TestClient.class, "https://api.example.com");

        // then
        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("HttpClientRegistry 应该能够注册和获取 HTTP 客户端")
    void shouldRegisterAndGetHttpClient() {
        // given
        HttpClientRegistry registry = getBean(HttpClientRegistry.class);

        // when
        TestClient client = registry.register("testClient", TestClient.class, "https://api.example.com");

        // then
        assertThat(client).isNotNull();
        assertThat(registry.hasClient("testClient")).isTrue();
        assertThat(registry.getClient("testClient", TestClient.class)).isNotNull();
    }

    @Test
    @DisplayName("所有 HTTP 客户端相关 Bean 应该在 ApplicationContext 中可用")
    void allHttpClientBeansShouldBeAvailable() {
        // when & then
        assertThat(applicationContext.containsBean("restClientBuilder")).isTrue();
        assertThat(applicationContext.containsBean("httpClientRegistry")).isTrue();
    }

    /**
     * 测试用的 HTTP 客户端接口
     */
    @org.springframework.web.service.annotation.HttpExchange("/test")
    interface TestClient {}
}
